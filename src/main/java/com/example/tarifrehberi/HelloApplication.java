package com.example.tarifrehberi;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
public class HelloApplication extends Application {
TextField arama;

@FXML
public ListView<Tarif> liste;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("AnaSayfa.fxml"));
        Parent root = fxmlLoader.load();
        ListView<Tarif> liste = (ListView<Tarif>) root.lookup("#liste");
        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Tarif Rehberi");
        stage.setScene(scene);
        stage.show();


        // DatabaseHelper'dan veritabanını başlatma ve tablo oluşturma işlemi
        DatabaseHelper db = new DatabaseHelper();
        //db.connect();
      // db.TarifEkle("Ben yaptım oldu", "Ana Yemek", 20,"Suya koy 10 dk bekle al süz ye"); // Tarif ekle

        if (liste != null) {
            ObservableList<Tarif> list = db.getTarifler();
            liste.setItems(list);

    }
}
    public static void main(String[] args) {
        launch();  // JavaFX uygulamasını başlat
    }
}