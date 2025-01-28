package com.example.learnit;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.*;

public class DeckManager {
    private final DatabaseManager databaseManager;
    private TreeView<String> deckTree;
    private TreeItem<String> rootNode;

    public DeckManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        // inicjalizacja drzewa deckow
        initializeTreeView();
    }

    public TreeView<String> getDeckTree() {
        // zwraca drzewo deckow
        return deckTree;
    }

    public void loadDeckHierarchy() {
        // upewnij sie, ze rootNode jest zainicjalizowany
        if (rootNode == null) {
            rootNode = new TreeItem<>("Root");
            deckTree.setRoot(rootNode);
            rootNode.setExpanded(true);
        }

        // czyszczenie istniejących węzłów w drzewie
        rootNode.getChildren().clear();

        // pobranie hierarchii deckow z bazy danych
        List<Deck> decks = databaseManager.getDeckHierarchy();
        Map<Integer, TreeItem<String>> deckMap = new HashMap<>();

        // budowanie drzewa na podstawie hierarchii deckow
        for (Deck deck : decks) {
            TreeItem<String> treeItem = new TreeItem<>(deck.getName());
            deckMap.put(deck.getId(), treeItem);

            if (deck.getParentId() == null) {
                // jesli deck nie ma rodzica, dodaj do rootNode
                rootNode.getChildren().add(treeItem);
            } else {
                // jesli deck ma rodzica, znajdz go w mapie i dodaj jako dziecko
                TreeItem<String> parentItem = deckMap.get(deck.getParentId());
                if (parentItem != null) {
                    parentItem.getChildren().add(treeItem);
                } else {
                    System.out.println("Parent not found for: " + deck.getName());
                }
            }
        }
    }

    private void initializeTreeView() {
        // inicjalizacja drzewa z rootNode
        rootNode = new TreeItem<>("Root");
        rootNode.setExpanded(true);
        deckTree = new TreeView<>(rootNode);

        // ukrycie glownego wezla root (opcja kosmetyczna)
        deckTree.setShowRoot(false);
    }

    public void addDeck(String name, Integer parentId) {
        // dodanie nowego decku do bazy i odswiezenie hierarchii
        databaseManager.addDeck(name, parentId);
        loadDeckHierarchy();
    }

    public void deleteDeck(String name) {
        // usuniecie decku na podstawie nazwy i odswiezenie hierarchii
        Deck deck = findDeckByName(name);
        if (deck != null) {
            databaseManager.deleteDeck(deck.getId());
            loadDeckHierarchy();
        }
    }

    public void editDeck(String currentName, String newName) {
        // edycja nazwy decku i odswiezenie hierarchii
        Deck deck = findDeckByName(currentName);
        if (deck != null) {
            databaseManager.updateDeck(deck.getId(), newName);
            loadDeckHierarchy();
        }
    }

    private Deck findDeckByName(String name) {
        // wyszukaj deck na podstawie nazwy
        List<Deck> decks = databaseManager.getDeckHierarchy();
        return decks.stream()
                .filter(deck -> deck.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
