module com.example.tarifrehberi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.tarifrehberi to javafx.fxml;
    exports com.example.tarifrehberi;
}