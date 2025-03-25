package com.example.learnit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphicManager {
    private final Stage primaryStage;
    private final DatabaseManager databaseManager;
    private final FlashcardManager flashcardManager;
    private VBox mainLayout;
    private HBox buttonBox;
    private TreeView<String> deckTree;
    private VBox treeContainer;
    private Stage flashcardStage;
    private int currentFlashcardIndex;
    private List<Flashcard> currentFlashcards;
    private Label flashcardLabel;
    private HBox flashcardButtons;
    private int currentDeckId;
    private int batchSize = 100;
    private int loadedFlashcardsOffset = 0;

    public GraphicManager(Stage primaryStage, DatabaseManager databaseManager, FlashcardManager flashcardManager) {
        this.primaryStage = primaryStage;
        this.databaseManager = databaseManager;
        this.flashcardManager = flashcardManager;

        initializeUI();
    }

    public Scene getMainScene() {
        return new Scene(mainLayout, 800, 600);
    }

    private void initializeUI() {
        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: #F0F0F0;");

        deckTree = createDeckTree();
        deckTree.setPrefHeight(500);
        deckTree.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: black; -fx-font-size: 14px; -fx-border-radius: 5px;");

        treeContainer = new VBox(deckTree);
        treeContainer.setPadding(new Insets(10));
        treeContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px;");

        Button addDeckButton = createModernButton("Add Deck");
        Button deleteDeckButton = createModernButton("Delete Deck");
        Button editDeckButton = createModernButton("Edit Deck");
        Button addFlashcardButton = createModernButton("Add Flashcard");

        addDeckButton.setOnAction(e -> addDeck());
        deleteDeckButton.setOnAction(e -> deleteDeck(deckTree));
        editDeckButton.setOnAction(e -> editDeck(deckTree));
        addFlashcardButton.setOnAction(e -> addFlashcard());

        buttonBox = new HBox(10, addDeckButton, deleteDeckButton, editDeckButton, addFlashcardButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-background-color: #D3D3D3; -fx-background-radius: 10px;");

        updateButtonStyles("#4682B4", "white");

        mainLayout.getChildren().addAll(treeContainer, buttonBox);
        VBox.setVgrow(treeContainer, Priority.ALWAYS);

        deckTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showFlashcards(newSelection);
            }
        });
    }

    private void updateButtonStyles(String backgroundColor, String textColor) {
        for (var node : buttonBox.getChildren()) {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.setStyle(
                        "-fx-background-color: " + backgroundColor + ";" +
                                "-fx-text-fill: " + textColor + ";" +
                                "-fx-font-size: 14px;" +
                                "-fx-padding: 10 20 10 20;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
                );
            }
        }
    }

    private Button createModernButton(String text) {
        return new Button(text);
    }

    private TreeView<String> createDeckTree() {
        TreeItem<String> rootNode = new TreeItem<>("");
        rootNode.setExpanded(true);

        for (Deck deck : databaseManager.getDeckHierarchy()) {
            int flashcardCount = databaseManager.getFlashcardCount(deck.getId());
            HBox deckBox = new HBox(10);
            deckBox.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(deck.getName());
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            Label countLabel = new Label(String.valueOf(flashcardCount));
            deckBox.getChildren().addAll(nameLabel, countLabel);

            TreeItem<String> deckItem = new TreeItem<>("");
            deckItem.setGraphic(deckBox);
            rootNode.getChildren().add(deckItem);
        }

        TreeView<String> treeView = new TreeView<>(rootNode);
        treeView.setShowRoot(false);
        return treeView;
    }

    private void addDeck() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Deck");
        dialog.setHeaderText("Enter deck name:");
        dialog.setContentText("Deck name:");
        applyDialogStyle(dialog);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                databaseManager.addDeck(name.trim(), null);
                updateDeckTree();
            }
        });
    }

    private void deleteDeck(TreeView<String> deckTree) {
        TreeItem<String> selectedItem = deckTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            HBox deckBox = (HBox) selectedItem.getGraphic();
            String deckName = ((Label) deckBox.getChildren().get(0)).getText();
            Deck deck = findDeckByName(deckName);
            if (deck != null) {
                databaseManager.deleteDeck(deck.getId());
                updateDeckTree();
            }
        } else {
            showAlert("Error", "Please select a deck to delete.");
        }
    }

    private void editDeck(TreeView<String> deckTree) {
        TreeItem<String> selectedItem = deckTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            HBox deckBox = (HBox) selectedItem.getGraphic();
            String currentName = ((Label) deckBox.getChildren().get(0)).getText();
            TextInputDialog dialog = new TextInputDialog(currentName);
            dialog.setTitle("Edit Deck");
            dialog.setHeaderText("Edit deck name:");
            dialog.setContentText("New name:");
            applyDialogStyle(dialog);

            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    Deck deck = findDeckByName(currentName);
                    if (deck != null) {
                        databaseManager.updateDeck(deck.getId(), newName.trim());
                        updateDeckTree();
                    }
                }
            });
        } else {
            showAlert("Error", "Please select a deck to edit.");
        }
    }

    private void addFlashcard() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Flashcard");
        dialog.setHeaderText("Add or import flashcards:");

        ComboBox<String> deckSelector = new ComboBox<>();
        for (Deck deck : databaseManager.getDeckHierarchy()) {
            deckSelector.getItems().add(deck.getName());
        }
        deckSelector.setPromptText("Select a deck");
        deckSelector.setPrefWidth(200);

        TextField frontField = new TextField();
        frontField.setPromptText("Enter front text");
        TextField backField = new TextField();
        backField.setPromptText("Enter back text");

        Button importButton = createModernButton("Import from CSV");
        importButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null && deckSelector.getValue() != null) {
                Deck selectedDeck = findDeckByName(deckSelector.getValue());
                if (selectedDeck != null) {
                    importFromCSV(file, selectedDeck.getId());
                    showAlert("Success", "Flashcards imported successfully!");
                    updateDeckTree();
                    if (flashcardStage != null && flashcardStage.isShowing() && currentDeckId == selectedDeck.getId()) {
                        loadNextFlashcardBatchIfNeeded();
                    }
                    dialog.close();
                }
            } else {
                showAlert("Error", "Please select a deck before importing!");
            }
        });

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                new Label("Deck:"), deckSelector,
                new Label("Front:"), frontField,
                new Label("Back:"), backField,
                importButton
        );
        content.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: black;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyDialogStyle(dialog);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String selectedDeckName = deckSelector.getValue();
                String frontText = frontField.getText().trim();
                String backText = backField.getText().trim();
                if (selectedDeckName != null && !frontText.isEmpty() && !backText.isEmpty()) {
                    Deck selectedDeck = findDeckByName(selectedDeckName);
                    if (selectedDeck != null) {
                        flashcardManager.addFlashcard(selectedDeck.getId(), frontText, backText);
                        showAlert("Success", "Flashcard added!");
                        updateDeckTree();
                        if (flashcardStage != null && flashcardStage.isShowing() && currentDeckId == selectedDeck.getId()) {
                            loadNextFlashcardBatchIfNeeded();
                        }
                    }
                } else if (frontText.isEmpty() && backText.isEmpty()) {
                    // Nic nie rób, jeśli pola są puste - import już obsłużony
                } else {
                    showAlert("Error", "Please select a deck and fill both front and back fields!");
                }
            }
        });
    }

    private void importFromCSV(File file, int deckId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String front = parts[0].trim();
                    String back = parts[1].trim();
                    if (!front.isEmpty() && !back.isEmpty()) {
                        flashcardManager.addFlashcard(deckId, front, back);
                    }
                }
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to import flashcards: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showFlashcards(TreeItem<String> selectedItem) {
        HBox deckBox = (HBox) selectedItem.getGraphic();
        String deckName = ((Label) deckBox.getChildren().get(0)).getText();
        Deck selectedDeck = findDeckByName(deckName);
        if (selectedDeck == null) return;

        currentDeckId = selectedDeck.getId();
        loadedFlashcardsOffset = 0;
        currentFlashcards = databaseManager.getFlashcardsBatch(selectedDeck.getId(), loadedFlashcardsOffset, batchSize);
        if (currentFlashcards.isEmpty()) {
            showAlert("Info", "No flashcards in this deck.");
            return;
        }

        currentFlashcardIndex = 0;
        if (flashcardStage == null || !flashcardStage.isShowing()) {
            flashcardStage = new Stage();
            flashcardStage.setTitle("Flashcards - " + selectedDeck.getName());

            VBox flashcardLayout = new VBox(20);
            flashcardLayout.setPadding(new Insets(20));
            flashcardLayout.setStyle("-fx-background-color: #FFFFFF;");
            flashcardLayout.setAlignment(Pos.CENTER);

            flashcardLabel = new Label(currentFlashcards.get(currentFlashcardIndex).getFront());
            flashcardLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
            flashcardLabel.setWrapText(true);

            Button showBackButton = createModernButton("Show Back");
            showBackButton.setOnAction(e -> showBack());

            flashcardButtons = new HBox(10, showBackButton);
            flashcardButtons.setAlignment(Pos.CENTER);
            updateButtonStyles("#4682B4", "white");

            flashcardLayout.getChildren().addAll(flashcardLabel, flashcardButtons);

            Scene flashcardScene = new Scene(flashcardLayout, 400, 300);
            flashcardStage.setScene(flashcardScene);
            flashcardStage.show();
        } else {
            flashcardLabel.setText(currentFlashcards.get(currentFlashcardIndex).getFront());
            flashcardButtons.getChildren().clear();
            Button showBackButton = createModernButton("Show Back");
            showBackButton.setOnAction(e -> showBack());
            flashcardButtons.getChildren().add(showBackButton);
            updateButtonStyles("#4682B4", "white");
        }
    }

    private void showBack() {
        flashcardLabel.setText(currentFlashcards.get(currentFlashcardIndex).getBack());
        flashcardButtons.getChildren().clear();

        Button showFrontButton = createModernButton("Show Front");
        showFrontButton.setOnAction(e -> showFront());

        Button nextFlashcardButton = createModernButton("Next Flashcard");
        nextFlashcardButton.setOnAction(e -> nextFlashcard());

        flashcardButtons.getChildren().addAll(showFrontButton, nextFlashcardButton);
        updateButtonStyles("#4682B4", "white");
    }

    private void showFront() {
        flashcardLabel.setText(currentFlashcards.get(currentFlashcardIndex).getFront());
        flashcardButtons.getChildren().clear();

        Button showBackButton = createModernButton("Show Back");
        showBackButton.setOnAction(e -> showBack());

        flashcardButtons.getChildren().add(showBackButton);
        updateButtonStyles("#4682B4", "white");
    }

    private void nextFlashcard() {
        currentFlashcardIndex++;
        if (currentFlashcardIndex >= currentFlashcards.size()) {
            loadNextFlashcardBatchIfNeeded();
            if (currentFlashcardIndex >= currentFlashcards.size()) {
                currentFlashcardIndex = 0;
            }
        }
        flashcardLabel.setText(currentFlashcards.get(currentFlashcardIndex).getFront());
        flashcardButtons.getChildren().clear();

        Button showBackButton = createModernButton("Show Back");
        showBackButton.setOnAction(e -> showBack());

        flashcardButtons.getChildren().add(showBackButton);
        updateButtonStyles("#4682B4", "white");
    }

    private void loadNextFlashcardBatchIfNeeded() {
        if (currentFlashcardIndex >= currentFlashcards.size() - 10) {
            loadedFlashcardsOffset += batchSize;
            List<Flashcard> nextBatch = databaseManager.getFlashcardsBatch(currentDeckId, loadedFlashcardsOffset, batchSize);
            if (!nextBatch.isEmpty()) {
                currentFlashcards.addAll(nextBatch);
            }
        }
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-radius: 10px;"
        );
        for (ButtonType buttonType : dialog.getDialogPane().getButtonTypes()) {
            Button button = (Button) dialog.getDialogPane().lookupButton(buttonType);
            button.setStyle(
                    "-fx-background-color: #4682B4;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-radius: 8px;"
            );
        }
    }

    private Deck findDeckByName(String name) {
        for (Deck deck : databaseManager.getDeckHierarchy()) {
            if (deck.getName().equals(name)) {
                return deck;
            }
        }
        return null;
    }

    private void updateDeckTree() {
        deckTree.getRoot().getChildren().clear();
        for (Deck deck : databaseManager.getDeckHierarchy()) {
            int flashcardCount = databaseManager.getFlashcardCount(deck.getId());
            HBox deckBox = new HBox(10);
            deckBox.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(deck.getName());
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            Label countLabel = new Label(String.valueOf(flashcardCount));
            deckBox.getChildren().addAll(nameLabel, countLabel);

            TreeItem<String> deckItem = new TreeItem<>("");
            deckItem.setGraphic(deckBox);
            deckTree.getRoot().getChildren().add(deckItem);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }
}