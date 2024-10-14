package com.example.tarifrehberi;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class TarifRehberi extends Application {//Application sınıfından türetilmiştir, bu da sınıfın bir JavaFX uygulaması olduğunu belirtir. JavaFX uygulamaları bu sınıftan türemelidir.

@FXML


    @Override
    public void start(Stage stage) throws IOException { //start Metodu: JavaFX uygulamasının başlangıç noktasıdır. Uygulama çalıştığında, bu metod çağrılır.
        FXMLLoader fxmlLoader = new FXMLLoader(TarifRehberi.class.getResource("AnaSayfa.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Tarif Rehberi");
        Image image = new Image(Objects.requireNonNull(TarifRehberi.class.getResource("/icons/CookingBook.png")).toExternalForm());
        stage.getIcons().add(image);
        stage.setScene(scene);
        stage.show();


        //db.connect();
      // db.TarifEkle("Ben yaptım oldu", "Ana Yemek", 20,"Suya koy 10 dk bekle al süz ye"); // Tarif ekle

}
    public static void main(String[] args) {

    launch();//launch() metodu, JavaFX uygulamasını başlatmak için kullanılır ve start metodunu çağırır.
    }
}