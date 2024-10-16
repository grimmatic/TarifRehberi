package com.example.tarifrehberi;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.util.*;


public class AnaSayfaController implements Initializable{


    @FXML
    private TextField arama;
    @FXML
    private GridPane recipeGrid;
    @FXML
    private  AnchorPane anchor;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private VBox filterVBox;
    @FXML
    private TreeView<HBox> treeView;
    @FXML
    public Button treeviewButton;
    private Stage stage;
    private Scene scene;
    private Parent root;
    int anchorYukeskligi = 0;
    private Connection conn;
    private UpdateController updateController;
    private TarifEkle tarifEkle;


    Database db = new Database();


    @Override
    public void initialize(URL url, ResourceBundle rb) {


        connectToDatabase();
        loadRecipes(""); // Başlangıçta tüm tarifleri yükler.
        // Boş bir arama terimi geçirildiği için,
        // veritabanından mevcut olan tüm tarifler alınır
        // ve arayüzde görüntülenir.
        // Bu, kullanıcı uygulamayı başlattığında
        // hemen tarifleri görmesini sağlar.
        updateController = new UpdateController(conn, db);
        tarifEkle = new TarifEkle(db, conn);





        if (arama != null) {
            arama.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    arama.setFocusTraversable(false);
                }
            });
        }

        arama.textProperty().addListener((observable, oldValue, newValue) -> loadRecipes(newValue)); // Kullanıcı arama alanına metin girdiğinde (veya metni değiştirdiğinde), loadRecipes(newValue) metodu çağrılır. Bu, veritabanındaki tariflerin arama terimine göre güncellenmesini sağlar. Örneğin, kullanıcı "pasta" yazarsa, yalnızca pasta tarifleri yüklenir.

        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadRecipes(arama.getText())); //sıralam kutucugu, en kısa en yavaş vs

        for (TreeItem<HBox> categoryItem : treeView.getRoot().getChildren()) { //kategoriye göre arama
            HBox hBox = categoryItem.getValue();
            CheckBox checkBox = (CheckBox) hBox.getChildren().get(0);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                loadRecipes(arama.getText());
            });
        }


        tarifEkle = new TarifEkle(db, conn);
        tarifEkle.setOnRecipeAddedCallback(() -> {
            showToast("Yeni tarif başarıyla eklendi.");
            loadRecipes(arama.getText());
        });



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
                    case "En Yavaş" -> query.append(" ORDER BY HazirlamaSuresi DESC");
                    case "En Çabuk" -> query.append(" ORDER BY HazirlamaSuresi ASC");
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
                int preparationTime = rs.getInt("HazirlamaSuresi");
                String Tarif = rs.getString("Talimatlar");
                int tarifID = rs.getInt("TarifID");

                //tarifleri görselleştirmece
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
                    anchorYukeskligi = 150*(int)Math.ceil((double)satir / 3);
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





                deleteButton.setOnAction(event -> { //delete butonuna tıklandıgında sil
                    db.deleteRecipe(conn, tarifID);
                    loadRecipes(arama.getText());
                });

                //updateButton.setOnAction(event -> showUpdateDialog(tarifID, recipeName, category, preparationTime, Tarif));
                updateButton.setOnAction(event -> {
                    // Butona basıldığında dialogu göster
                    updateController.showUpdateDialog(tarifID, recipeName, category, preparationTime, Tarif);
                    updateController.setOnRecipeUpdatedCallback(() -> {
                        showToast("Tarif başarıyla güncellendi.");
                        loadRecipes(arama.getText());
                    });

                });


                buttonBox.getChildren().addAll(updateButton, deleteButton);
                recipeBox.getChildren().add(buttonBox);
                recipeGrid.add(recipeBox, column, row);

                recipeBox.setOnMouseClicked(event -> displayRecipeDetails(tarifID,recipeName, category, preparationTime, Tarif)); //tarife tıklandıgında detay cıksın
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

    private void displayRecipeDetails(int tarifID, String recipeName, String category, int preparationTime, String instructions) {
        UpdateController updateController = new UpdateController(conn, db); // Burada doğru bağlantıyı sağlayın.



        recipeGrid.setVisible(false);//Mevcut tariflerin bulunduğu ızgarayı (grid) görünmez hale getirir.
        // Böylece tarif detayları görünür olduğunda mevcut tarifler gizlenmiş olur.


        AnchorPane detailPane = new AnchorPane();
        detailPane.setStyle("-fx-background-color: #FFFFFF;");
        detailPane.setPadding(new Insets(20));


        Image backgroundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/recipebackground.jpg")));
        ImageView backgroundImageView = new ImageView(backgroundImage);
        backgroundImageView.setFitWidth(1000);
        backgroundImageView.setFitHeight(615);
        //  backgroundImageView.setPreserveRatio(true);
        backgroundImageView.setOpacity(0.15);
        detailPane.getChildren().add(backgroundImageView);

        Label nameLabel = new Label(recipeName);
        nameLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label categoryLabel = new Label("Kategori: " + category);
        categoryLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label timeLabel = new Label("Hazırlama Süresi: " + preparationTime + " dakika");
        timeLabel.setStyle("-fx-font-size: 16px");

        Label instructionsLabel = new Label("Talimatlar");
        instructionsLabel.setStyle("-fx-font-size: 16px;-fx-font-weight: bold;");
        instructionsLabel.setPadding(new Insets(100, 0, 0, 140));
        Separator separator = new Separator();
        separator.setPrefWidth(300);


        Label instructionsLabel0 = new Label( formatInstructions(instructions));
        instructionsLabel0.setStyle("-fx-font-size: 14px;-fx-font-weight: bold;");
        instructionsLabel0.setWrapText(true);
        instructionsLabel0.setMaxWidth(350);

        VBox vbox = new VBox(10, nameLabel, categoryLabel, timeLabel,instructionsLabel,separator,instructionsLabel0);
        vbox.setPadding(new Insets(20));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editButton = new Button();
        editButton.setStyle("-fx-background-color:transparent;");
        ImageView imageView0 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/rewrite.png"))));
        imageView0.setFitHeight(40);
        imageView0.setFitWidth(40);
        editButton.setMaxWidth(30);
        editButton.setMaxHeight(30);
        editButton.setGraphic(imageView0);

        Button deleteButton = new Button();
        deleteButton.setStyle("-fx-background-color:transparent;");
        ImageView imageView1 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/delete.png"))));
        imageView1.setFitHeight(40);
        imageView1.setFitWidth(40);
        deleteButton.setMaxWidth(30);
        deleteButton.setMaxHeight(30);
        deleteButton.setGraphic(imageView1);

        Button homeButton = new Button();
        homeButton.setStyle("-fx-background-color:transparent;");
        ImageView imageView2 = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/home.png"))));
        imageView2.setFitHeight(40);
        imageView2.setFitWidth(40);
        homeButton.setMaxWidth(40);
        homeButton.setMaxHeight(40);
        homeButton.setGraphic(imageView2);


        buttonBox.getChildren().addAll(editButton, deleteButton, homeButton);
        vbox.getChildren().add(buttonBox);


        homeButton.setOnAction(event -> { //Bu butona tıklandığında, detayları gösteren detailPane kaldırılır
            // ve tariflerin listesi yeniden yüklenir.
            // Ayrıca diğer arayüz bileşenleri görünür hale gelir.
            anchor.setPrefHeight(anchorYukeskligi);
            anchor.setMinHeight(anchorYukeskligi);
            sortComboBox.setVisible(true);
            arama.setVisible(true);
            treeviewButton.setVisible(true);


            anchor.getChildren().remove(detailPane);
            loadRecipes(arama.getText());
            recipeGrid.setVisible(true);
        });

//        editButton.setOnAction(event -> {
//            showUpdateDialog(tarifID, recipeName, category, preparationTime, instructions);

        editButton.setOnAction(event -> {
            // UpdateController'daki showUpdateDialog metodunu çağır
            updateController.showUpdateDialog(tarifID, recipeName, category, preparationTime, instructions);
            loadRecipes(arama.getText());











            // Detay görünümünden çıkış ve listeyi tekrar yüklemek için:
            anchor.setPrefHeight(anchorYukeskligi);
            anchor.setMinHeight(anchorYukeskligi);
            sortComboBox.setVisible(true);
            arama.setVisible(true);
            treeviewButton.setVisible(true);
            anchor.getChildren().remove(detailPane);
            recipeGrid.setVisible(true);
        });



        deleteButton.setOnAction(event -> { //Silme İşlemi: Bu butona tıklandığında,
            // tarif veritabanından silinir ve tariflerin listesi yeniden yüklenir.
            db.deleteRecipe(conn, tarifID);
            loadRecipes(arama.getText());
            anchor.setPrefHeight(anchorYukeskligi);
            anchor.setMinHeight(anchorYukeskligi);
            sortComboBox.setVisible(true);
            arama.setVisible(true);
            treeviewButton.setVisible(true);
            anchor.getChildren().remove(detailPane);
            recipeGrid.setVisible(true);
        });




        detailPane.getChildren().add(vbox);

        AnchorPane.setTopAnchor(detailPane, 0.0);
        AnchorPane.setRightAnchor(detailPane, 0.0);
        AnchorPane.setBottomAnchor(detailPane, 0.0);
        AnchorPane.setLeftAnchor(detailPane, 0.0);

        anchor.setPrefHeight(615);
        anchor.setMinHeight(615);
        anchor.setMaxHeight(615);
        sortComboBox.setVisible(false);
        arama.setVisible(false);
        treeviewButton.setVisible(false);


        anchor.getChildren().add(detailPane);
    }





    @FXML
    private void handleTreeViewButtonAction(ActionEvent event) {
        filterVBox.setVisible(!filterVBox.isVisible());
    }

    private String getSelectedCategories() { //kullanıcı tarafından seçilen kategorileri almak için kullanılır.
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


    @FXML
    private void handleAddRecipe() {
        tarifEkle.showAddRecipeDialog();
    }



    private String formatInstructions(String instructions) {
        StringBuilder formattedInstructions = new StringBuilder();
        String[] lines = instructions.split("\\d+\\.");

        for (int i = 1; i < lines.length; i++) {
            formattedInstructions.append(i).append(".").append(lines[i].trim()).append("\n");
        }

        return formattedInstructions.toString().trim();
    }



    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void showToast(String message) {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Label label = new Label(message);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); " +
                "-fx-text-fill: white; " +
                "-fx-padding: 10px; " +
                "-fx-border-radius: 5px; ");

        popup.getContent().add(label);

        Stage stage = (Stage) anchor.getScene().getWindow();

        popup.setOnShown(e -> {
            popup.setX(stage.getX() + stage.getWidth()/2 - popup.getWidth()/2);
            popup.setY(stage.getY() + stage.getHeight() - 50);
        });

        Platform.runLater(() -> {
            popup.show(stage);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(popup::hide);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }



}
