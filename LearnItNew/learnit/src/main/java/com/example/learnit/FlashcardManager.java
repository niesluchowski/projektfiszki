package com.example.learnit;

public class FlashcardManager {
    private final DatabaseManager databaseManager;

    // Konstruktor, który przyjmuje instancję DatabaseManager
    public FlashcardManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Dodanie nowej fiszki do bazy danych.
     * @param deckId ID decku, do którego należy fiszka
     * @param front Tekst na przedniej stronie fiszki
     * @param back Tekst na tylnej stronie fiszki
     */
    public void addFlashcard(int deckId, String front, String back) {
        if (deckId <= 0 || front == null || front.isEmpty() || back == null || back.isEmpty()) {
            System.err.println("Invalid data for flashcard.");
            return;
        }

        // Wywołanie metody z DatabaseManager do dodania fiszki
        databaseManager.addFlashcard(deckId, front.trim(), back.trim());
        System.out.println("Flashcard added to deck ID " + deckId + ": [Front: " + front + ", Back: " + back + "]");
    }
}
