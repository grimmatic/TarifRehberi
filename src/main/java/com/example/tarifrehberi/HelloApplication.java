package com.example.tarifrehberi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
TextField arama;
    @Override
    public void start(Stage stage) throws IOException {
        // DatabaseHelper'dan veritabanını başlatma ve tablo oluşturma işlemi
        DatabaseHelper db = new DatabaseHelper();
     //   db.TarifEkle("Ben yaptım oldu", "Ana Yemek", 20,"Suya koy 10 dk bekle al süz ye"); // Tarif ekle



        // JavaFX GUI yükleniyor
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("AnaSayfa.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        stage.setTitle("Tarif Rehberi");
        stage.setScene(scene);
        stage.show();






    }

    public static void main(String[] args) {
        launch();  // JavaFX uygulamasını başlat
    }
}