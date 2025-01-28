module com.example.learnit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.learnit to javafx.fxml;
    exports com.example.learnit;
}