package com.example.tarifrehberi;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class AnaSayfaController implements Initializable{

    @FXML
    private TextField arama;
    @FXML
    private GridPane recipeGrid;
    @FXML
    private AnchorPane anchor;
    private Stage stage;
    private Scene scene;
    private Parent root;

    Database db = new Database();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        if (arama != null) {
            arama.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    arama.setFocusTraversable(false);
                }
            });
        }
        loadRecipes("");
       // arama.setOnAction(this::aramaYap);
        arama.textProperty().addListener((observable, oldValue, newValue) -> {

                loadRecipes(newValue);

        });
    }

    private void loadRecipes(String search) {
        int satir=0;
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
            Statement stmt = conn.createStatement();
            String query;
            if (search.isEmpty()) {
                query = "SELECT * FROM Tarifler";
            } else {
                query = "SELECT * FROM Tarifler WHERE TarifAdi LIKE '%" + search + "%'";
            }
            ResultSet rs = stmt.executeQuery(query);
            recipeGrid.getChildren().clear();
         //   recipeGrid.getRowConstraints().clear();
            int row = 0;
            int column = 0;
            while (rs.next()) {
                satir++;
                String recipeName = rs.getString("TarifAdi");
                String category = rs.getString("Kategori");
                int preparationTime = rs.getInt("HazirlanmaSuresi");
                String Tarif = rs.getString("Talimatlar");
                int tarifID = rs.getInt("TarifID");


                Label nameLabel = new Label(recipeName);
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                Label categoryLabel = new Label(category);
                Label timeLabel = new Label(preparationTime + " dakika");


                VBox recipeBox = new VBox(nameLabel, categoryLabel, timeLabel);
               // recipeBox.setSpacing(5);
                //recipeBox.setPrefHeight(100);
                // recipeBox.setPrefWidth(100);
                for (int i = 0; i < (int)Math.ceil((double)satir / 3); i++){

                    RowConstraints rowConstraints = new RowConstraints();
                    rowConstraints.setMinHeight(150.0);
                    rowConstraints.setPrefHeight(150.0);
                    recipeGrid.getRowConstraints().add(rowConstraints);
                    anchor.setPrefHeight(150*(int)Math.ceil((double)satir / 3));
                }

                HBox buttonBox = new HBox(10);
                Button deleteButton = new Button();
                deleteButton.setStyle("-fx-background-color:transparent;");
                deleteButton.setGraphicTextGap(10);
                ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/delete.png")));
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                deleteButton.setMaxWidth(30);
                deleteButton.setMaxHeight(30);
                deleteButton.setGraphic(imageView);
                deleteButton.setPadding(new Insets(20, 0, 0, 0));


                Button updateButton = new Button();
                updateButton.setStyle("-fx-background-color:transparent;");
                updateButton.setGraphicTextGap(10);
                ImageView imageView0 = new ImageView(new Image(getClass().getResourceAsStream("/icons/rewrite.png")));
                imageView0.setFitHeight(40);
                imageView0.setFitWidth(40);
                updateButton.setMaxWidth(30);
                updateButton.setMaxHeight(30);
                updateButton.setGraphic(imageView0);
                updateButton.setPadding(new Insets(25, 0, 0, 0));


                deleteButton.setOnAction(event -> {
                    db.deleteRecipe(conn, tarifID);
                    loadRecipes(arama.getText());
                });



                buttonBox.getChildren().addAll(updateButton, deleteButton);
                recipeBox.getChildren().add(buttonBox);
                recipeGrid.add(recipeBox, column, row);
              //  recipeGrid.setConstraints(recipeBox, column, row);
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
                if (column == 3) {
                    column = 0;
                    row++;
                }
            }

           // rs.close();
           // stmt.close();
           // conn.close();
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
    private void aramaYap(ActionEvent event) {
        String aramaMetni = arama.getText();
        if (!aramaMetni.isEmpty()) {
            aramaSonucunuGoster(aramaMetni);
        }
    }


    private void aramaSonucunuGoster(String aramaMetni) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Tarifler WHERE TarifAdi LIKE '%" + aramaMetni + "%'");

            // Gridpane'de sonuçları göster
            recipeGrid.getChildren().clear();
            int row = 0;
            int column = 0;
            while (rs.next()) {
                String tarifAdi = rs.getString("TarifAdi");
                Label nameLabel = new Label(tarifAdi);
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                recipeGrid.add(nameLabel, column, row);
                column++;
                if (column > 2) {
                    column = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            // Hata işleme
        }
    }



}
