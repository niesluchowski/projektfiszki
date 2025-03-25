package com.example.learnit;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/learnit";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "";
    private final Map<Integer, Deck> deckCache;

    public DatabaseManager() {
        deckCache = new HashMap<>();
        initializeDatabase();
        loadDeckHierarchyToCache();
    }

    private void initializeDatabase() {
        String createDecksTableSQL = """
                CREATE TABLE IF NOT EXISTS decks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    parent_id INT DEFAULT NULL,
                    UNIQUE (name, parent_id),
                    FOREIGN KEY (parent_id) REFERENCES decks(id) ON DELETE CASCADE
                );
                """;

        String createFlashcardsTableSQL = """
                CREATE TABLE IF NOT EXISTS flashcards (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    deck_id INT NOT NULL,
                    front TEXT NOT NULL,
                    back TEXT NOT NULL,
                    FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
                );
                """;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(createDecksTableSQL);
            statement.execute(createFlashcardsTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addFlashcard(int deckId, String front, String back) {
        String sql = "INSERT INTO flashcards (deck_id, front, back) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, deckId);
            preparedStatement.setString(2, front);
            preparedStatement.setString(3, back);
            preparedStatement.executeUpdate();
            System.out.println("Flashcard added: [Deck ID: " + deckId + ", Front: " + front + ", Back: " + back + "]");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getFlashcardCount(int deckId) {
        String sql = "SELECT COUNT(*) FROM flashcards WHERE deck_id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, deckId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Flashcard> getFlashcardsBatch(int deckId, int offset, int limit) {
        List<Flashcard> flashcards = new ArrayList<>();
        String sql = "SELECT front, back FROM flashcards WHERE deck_id = ? LIMIT ?, ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, deckId);
            preparedStatement.setInt(2, offset);
            preparedStatement.setInt(3, limit);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String front = resultSet.getString("front");
                String back = resultSet.getString("back");
                flashcards.add(new Flashcard(front, back));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flashcards;
    }

    public List<Flashcard> getFlashcards(int deckId) { // Zachowana dla kompatybilności, ale nie używana w optymalizacji
        List<Flashcard> flashcards = new ArrayList<>();
        String sql = "SELECT front, back FROM flashcards WHERE deck_id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, deckId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String front = resultSet.getString("front");
                String back = resultSet.getString("back");
                flashcards.add(new Flashcard(front, back));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flashcards;
    }

    public List<Deck> getDeckHierarchy() {
        return new ArrayList<>(deckCache.values());
    }

    private void loadDeckHierarchyToCache() {
        deckCache.clear();
        String sql = "SELECT id, name, parent_id FROM decks";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                Integer parentId = resultSet.getObject("parent_id") != null ? resultSet.getInt("parent_id") : null;
                deckCache.put(id, new Deck(id, name, parentId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDeck(String name, Integer parentId) {
        String insertSQL = "INSERT INTO decks (name, parent_id) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, name);
            if (parentId != null) {
                preparedStatement.setInt(2, parentId);
            } else {
                preparedStatement.setNull(2, Types.INTEGER);
            }
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int id = generatedKeys.getInt(1);
                deckCache.put(id, new Deck(id, name, parentId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteDeck(int id) {
        String deleteSQL = "DELETE FROM decks WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(deleteSQL)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
            deckCache.remove(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDeck(int id, String newName) {
        String updateSQL = "UPDATE decks SET name = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
            Deck deck = deckCache.get(id);
            if (deck != null) {
                deckCache.put(id, new Deck(id, newName, deck.getParentId()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}