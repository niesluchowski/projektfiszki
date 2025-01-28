package com.example.learnit;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Tworzenie instancji DatabaseManager
        DatabaseManager databaseManager = new DatabaseManager();

        // Tworzenie instancji FlashcardManager
        FlashcardManager flashcardManager = new FlashcardManager(databaseManager);

        // Tworzenie instancji GraphicManager
        GraphicManager graphicManager = new GraphicManager(primaryStage, databaseManager, flashcardManager);

        // Ustawienie sceny i uruchomienie aplikacji
        primaryStage.setScene(graphicManager.getMainScene());
        primaryStage.setTitle("Deck Manager");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
