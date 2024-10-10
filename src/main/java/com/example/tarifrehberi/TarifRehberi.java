package com.example.tarifrehberi;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
public class TarifRehberi extends Application {

@FXML


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TarifRehberi.class.getResource("AnaSayfa.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Tarif Rehberi");
        stage.setScene(scene);
        stage.show();


        // Database'dan veritabanını başlatma ve tablo oluşturma işlemi

        //db.connect();
      // db.TarifEkle("Ben yaptım oldu", "Ana Yemek", 20,"Suya koy 10 dk bekle al süz ye"); // Tarif ekle

}
    public static void main(String[] args) {
        launch();  // JavaFX uygulamasını başlat
    }
}