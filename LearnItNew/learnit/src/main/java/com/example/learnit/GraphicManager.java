package com.example.learnit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GraphicManager {
    private final Stage primaryStage;
    private final DatabaseManager databaseManager;
    private final FlashcardManager flashcardManager; // Użycie FlashcardManager do zarządzania fiszkami
    private VBox mainLayout;

    public GraphicManager(Stage primaryStage, DatabaseManager databaseManager, FlashcardManager flashcardManager) {
        this.primaryStage = primaryStage;
        this.databaseManager = databaseManager;
        this.flashcardManager = flashcardManager;

        // Inicjalizacja interfejsu użytkownika
        initializeUI();
    }

    public Scene getMainScene() {
        // Zwraca główną scenę aplikacji
        return new Scene(mainLayout, 800, 600);
    }

    private void initializeUI() {
        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Tworzenie drzewa decków
        TreeView<String> deckTree = createDeckTree();
        deckTree.setPrefHeight(500);

        VBox treeContainer = new VBox(deckTree);
        treeContainer.setPadding(new Insets(10));

        // Przyciski do zarządzania deckami i fiszkami
        Button addDeckButton = new Button("Add Deck");
        Button deleteDeckButton = new Button("Delete Deck");
        Button editDeckButton = new Button("Edit Deck");
        Button addFlashcardButton = new Button("Add Flashcard");

        // Przypisanie akcji do przycisków
        addDeckButton.setOnAction(e -> addDeck());
        deleteDeckButton.setOnAction(e -> deleteDeck(deckTree));
        editDeckButton.setOnAction(e -> editDeck(deckTree));
        addFlashcardButton.setOnAction(e -> addFlashcard(deckTree));

        // Dodanie przycisków do layoutu
        HBox buttonBox = new HBox(10, addDeckButton, deleteDeckButton, editDeckButton, addFlashcardButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        // Dodanie komponentów do głównego layoutu
        mainLayout.getChildren().addAll(treeContainer, buttonBox);
        VBox.setVgrow(treeContainer, Priority.ALWAYS);
    }

    private TreeView<String> createDeckTree() {
        // Tworzenie drzewa decków na podstawie danych z bazy
        TreeItem<String> rootNode = new TreeItem<>("Root");
        rootNode.setExpanded(true);

        for (Deck deck : databaseManager.getDeckHierarchy()) {
            TreeItem<String> deckItem = new TreeItem<>(deck.getName());
            rootNode.getChildren().add(deckItem);
        }

        TreeView<String> treeView = new TreeView<>(rootNode);
        treeView.setShowRoot(false); // Ukrycie głównego węzła root
        return treeView;
    }

    private void addDeck() {
        // Dialog do wprowadzenia nazwy nowego decku
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Deck");
        dialog.setHeaderText("Enter deck name:");
        dialog.setContentText("Deck name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                databaseManager.addDeck(name.trim(), null); // Dodanie decku na poziomie root
                refreshDeckTree();
            }
        });
    }

    private void deleteDeck(TreeView<String> deckTree) {
        // Usuwanie wybranego decku
        TreeItem<String> selectedItem = deckTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Deck deck = findDeckByName(selectedItem.getValue());
            if (deck != null) {
                databaseManager.deleteDeck(deck.getId());
                refreshDeckTree();
            }
        } else {
            showAlert("Error", "Please select a deck to delete.");
        }
    }

    private void editDeck(TreeView<String> deckTree) {
        // Edycja wybranego decku
        TreeItem<String> selectedItem = deckTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            TextInputDialog dialog = new TextInputDialog(selectedItem.getValue());
            dialog.setTitle("Edit Deck");
            dialog.setHeaderText("Edit deck name:");
            dialog.setContentText("New name:");

            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    Deck deck = findDeckByName(selectedItem.getValue());
                    if (deck != null) {
                        databaseManager.updateDeck(deck.getId(), newName.trim());
                        refreshDeckTree();
                    }
                }
            });
        } else {
            showAlert("Error", "Please select a deck to edit.");
        }
    }

    private void addFlashcard(TreeView<String> deckTree) {
        // Dodanie nowej fiszki do wybranego decku
        TreeItem<String> selectedItem = deckTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Dialog do wprowadzenia frontu fiszki
            TextInputDialog frontDialog = new TextInputDialog();
            frontDialog.setTitle("Add Flashcard");
            frontDialog.setHeaderText("Enter the front text:");
            frontDialog.setContentText("Front:");

            frontDialog.showAndWait().ifPresent(frontText -> {
                // Dialog do wprowadzenia tyłu fiszki
                TextInputDialog backDialog = new TextInputDialog();
                backDialog.setTitle("Add Flashcard");
                backDialog.setHeaderText("Enter the back text:");
                backDialog.setContentText("Back:");

                backDialog.showAndWait().ifPresent(backText -> {
                    // Znajdź deck po nazwie
                    Deck selectedDeck = findDeckByName(selectedItem.getValue());
                    if (selectedDeck != null) {
                        // Dodanie fiszki do bazy danych za pomocą FlashcardManager
                        flashcardManager.addFlashcard(selectedDeck.getId(), frontText, backText);
                        showAlert("Success", "Flashcard added!");
                    } else {
                        showAlert("Error", "Deck not found!");
                    }
                });
            });
        } else {
            showAlert("Error", "Please select a deck to add a flashcard.");
        }
    }

    private Deck findDeckByName(String name) {
        // Znajdź deck po nazwie
        for (Deck deck : databaseManager.getDeckHierarchy()) {
            if (deck.getName().equals(name)) {
                return deck;
            }
        }
        return null;
    }

    private void refreshDeckTree() {
        // Ponowna inicjalizacja interfejsu użytkownika
        initializeUI();

        // Ustawienie głównej sceny na nowo
        primaryStage.setScene(new Scene(mainLayout, 800, 600));
    }


    private void showAlert(String title, String message) {
        // Wyświetlenie alertu z podanym tytułem i wiadomością
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
