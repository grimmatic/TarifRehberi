package com.example.tarifrehberi;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class AnaSayfaController implements Initializable{
    @FXML
    private Label welcomeText;
    public Button deneme;
    public TextField arama;
    private Stage stage;
    private Scene scene;
    private Parent root;


    @FXML
    private GridPane recipeGrid;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
      //  deneme.setText("Yok et");
       // arama=new TextField();


        if (arama != null) {
            arama.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    arama.setFocusTraversable(false);
                }
            });
        }
        loadRecipes();


    }





    private void loadRecipes() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Tarifler");

            int row = 0;
            int column = 0;
            while (rs.next()) {
                String recipeName = rs.getString("TarifAdi");
                String category = rs.getString("Kategori");
                int preparationTime = rs.getInt("HazirlanmaSuresi");
                String Tarif = rs.getString("Talimatlar");


                Label nameLabel = new Label(recipeName);
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                Label categoryLabel = new Label(category);
                Label timeLabel = new Label(preparationTime + " dakika");


                VBox recipeBox = new VBox(nameLabel, categoryLabel, timeLabel);
                recipeBox.setSpacing(5); // Optional: spacing between labels
                recipeBox.setPrefHeight(100);
                recipeBox.setPrefWidth(100);

                recipeGrid.add(recipeBox, column, row);
                recipeBox.setOnMouseClicked(event -> {

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("TarifDetayları.fxml"));
                        Parent root = loader.load();

                        // Kontrolörü al ve tarif bilgilerini ayarla
                        TarifDetaylarıController controller = loader.getController();
                        controller.initialize(recipeName, category, preparationTime,Tarif);

                        Stage stage = (Stage) recipeBox.getScene().getWindow();
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }





                    System.out.println("Tarif detayları: " + recipeName + ", " + category + ", " + preparationTime + " dakika");
                });

                column++;
                if (column == 4) {
                    column = 0;
                    row++;
                }
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


   /* public void switchToAnaSayfa(ActionEvent event) throws Exception {
        root = FXMLLoader.load(getClass().getResource("AnaSayfa.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }*/

    public void switchToTarifDetayları(ActionEvent event) throws Exception {
        root = FXMLLoader.load(getClass().getResource("TarifDetayları.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToTarifEkle(ActionEvent event) throws Exception {
        root = FXMLLoader.load(getClass().getResource("TarifEkle.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }





    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
    @FXML
    protected void onButon0ButtonClick(){welcomeText.setText("");}


}
