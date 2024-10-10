package com.example.tarifrehberi;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;



public class AnaSayfaController implements Initializable{

    @FXML
    private TextField arama;
    @FXML
    private GridPane recipeGrid;
    @FXML
    private AnchorPane anchor;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private VBox filterVBox;
    @FXML
    private TreeView<HBox> treeView;
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
        arama.textProperty().addListener((observable, oldValue, newValue) -> loadRecipes(newValue));

        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadRecipes(arama.getText()));

        for (TreeItem<HBox> categoryItem : treeView.getRoot().getChildren()) {
            HBox hBox = categoryItem.getValue();
            CheckBox checkBox = (CheckBox) hBox.getChildren().get(0);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                loadRecipes(arama.getText());
            });
        }



    }

    private void loadRecipes(String search) {
        int satir=0;
        String sort = sortComboBox.getSelectionModel().getSelectedItem();
        String categoryFilter = getSelectedCategories();
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            recipeGrid.getChildren().clear();
            return;
        }

        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
            Statement stmt = conn.createStatement();
            StringBuilder query = new StringBuilder("SELECT * FROM Tarifler");
            boolean hasCondition = false;
            if (!search.isEmpty()) {
                query.append(" WHERE TarifAdi LIKE '%").append(search).append("%'");
                hasCondition = true;
            }

            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                if (hasCondition) {
                    query.append(" AND");
                } else {
                    query.append(" WHERE");
                    hasCondition = true;
                }
                query.append(" Kategori IN ('").append(categoryFilter.replace(", ", "', '")).append("')");
            }


            if (sort != null) {
                switch (sort) {
                    case "-" -> query.append(" ORDER BY TarifID ASC");
                    case "En Yavaş" -> query.append(" ORDER BY HazirlanmaSuresi DESC");
                    case "En Çabuk" -> query.append(" ORDER BY HazirlanmaSuresi ASC");
                }
            }



         //   recipeGrid.getRowConstraints().clear();
            ResultSet rs = stmt.executeQuery(query.toString());
            recipeGrid.getChildren().clear();
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
                for (int i = 0; i < (int)Math.ceil((double)satir / 3); i++){

                    RowConstraints rowConstraints = new RowConstraints();
                    rowConstraints.setMinHeight(150.0);
                    rowConstraints.setPrefHeight(150.0);
                    recipeGrid.getRowConstraints().add(rowConstraints);
                    anchor.setPrefHeight(150*(int)Math.ceil((double)satir / 3));
                }

                HBox buttonBox = new HBox(30);
                Button deleteButton = new Button();
                deleteButton.setStyle("-fx-background-color:transparent;");
                ImageView imageView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/delete.png"))));
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                deleteButton.setMaxWidth(30);
                deleteButton.setMaxHeight(30);
                deleteButton.setGraphic(imageView);
                deleteButton.setPadding(new Insets(20, 0, 0, 0));


                Button updateButton = new Button();
                updateButton.setStyle("-fx-background-color:transparent;");
                ImageView imageView0 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/rewrite.png"))));
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
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("TarifDetaylari.fxml"));
                        Parent root = loader.load();

                        TarifDetaylariController controller = loader.getController();
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
    @FXML
    private void handleTreeViewButtonAction(ActionEvent event) {
        filterVBox.setVisible(!filterVBox.isVisible());
    }

    private String getSelectedCategories() {
        StringBuilder categories = new StringBuilder();

        for (TreeItem<HBox> categoryItem : treeView.getRoot().getChildren()) {
            HBox hBox = categoryItem.getValue();
            CheckBox checkBox = (CheckBox) hBox.getChildren().get(0);

            if (checkBox.isSelected()) {
                if (categories.length() > 0) {
                    categories.append(", ");
                }
                categories.append(((Label) hBox.getChildren().get(1)).getText());
            }
        }

        return categories.length() > 0 ? categories.toString() : null;
    }

   /* public void switchToAnaSayfa(ActionEvent event) throws Exception {
        root = FXMLLoader.load(getClass().getResource("AnaSayfa.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }*/

    public void switchToTarifDetayları(ActionEvent event) throws Exception {
        root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("TarifDetaylari.fxml")));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToTarifEkle(ActionEvent event) throws Exception {
        root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("TarifEkle.fxml")));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }




}
