package com.example.tarifrehberi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.stage.Stage;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
    @FXML
    public Button treeviewButton;
    private Stage stage;
    private Scene scene;
    private Parent root;
    int anchorYukeskligi = 0;
    private Connection conn;

    Database db = new Database();

    @Override
    public void initialize(URL url, ResourceBundle rb) {


        connectToDatabase();
        db.createTable(conn);
        loadRecipes(""); // Başlangıçta tüm tarifleri yükler.
        // Boş bir arama terimi geçirildiği için,
        // veritabanından mevcut olan tüm tarifler alınır
        // ve arayüzde görüntülenir.
        // Bu, kullanıcı uygulamayı başlattığında
        // hemen tarifleri görmesini sağlar.




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

                updateButton.setOnAction(event -> showUpdateDialog(tarifID, recipeName, category, preparationTime, Tarif));


                buttonBox.getChildren().addAll(updateButton, deleteButton);
                recipeBox.getChildren().add(buttonBox);
                recipeGrid.add(recipeBox, column, row);
                //  recipeGrid.setConstraints(recipeBox, column, row);

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


        Label instructionsLabel0 = new Label( instructions);
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

        editButton.setOnAction(event -> {
            showUpdateDialog(tarifID, recipeName, category, preparationTime, instructions);
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

    private void showUpdateDialog(int tarifID, String recipeName, String category, int preparationTime, String instructions) {
        // Güncelleme için bir dialog oluştur
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tarifi Güncelle");

        // Resmi oluştur
        Image image = new Image(getClass().getResourceAsStream("/icons/rewrite.png"));
        ImageView imageView2 = new ImageView(image);
        imageView2.setFitWidth(50);
        imageView2.setFitHeight(40);

        // Özelleştirilmiş bir header Label'ı oluştur
        Label headerLabel = new Label("Tarifi güncellemek için bilgileri girin:");
        headerLabel.setStyle("-fx-background-color: #7a1e9c; -fx-text-fill: white; -fx-padding: 10; -fx-font-size: 16px;");
        headerLabel.setAlignment(Pos.CENTER);

        // Resmi sağa kaydırmak için bir Region ekleyin
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Header kısmına hem metni, hem boşluğu hem de resmi koymak için HBox kullanın
        HBox header = new HBox(10);
        header.getChildren().addAll(headerLabel, spacer, imageView2);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #7a1e9c; -fx-padding: 10;");
        header.setMaxWidth(Double.MAX_VALUE);

        // Dialog'un header alanına bu HBox'ı ekleyin
        dialog.getDialogPane().setHeader(header);

        // Dialog arka plan rengini mor yap
        dialog.getDialogPane().setStyle("-fx-background-color: #efe3f2;");

        // Tarifi düzenlemek için alanlar
        TextField nameField = new TextField(recipeName);
        ComboBox<String> categoryComboBox = new ComboBox<>();

        // Kategorileri veritabanından çekme ve ComboBox'a ekleme
        List<String> categories = db.getCategoriesFromRecipes(conn);
        categoryComboBox.getItems().addAll(categories);
        categoryComboBox.setValue(category); // Mevcut kategoriyi seçili olarak ayarlayın

        TextField preparationTimeField = new TextField(String.valueOf(preparationTime));
        TextArea instructionsField = new TextArea(instructions);

        // Malzemeler için ListView ve düğmeler
        ListView<String> ingredientsListView = new ListView<>();
        ingredientsListView.setPrefHeight(150); // Yükseklik belirlemek için

        // Malzemeleri tarifID'ye göre doldurma
        List<String> ingredients = db.getIngredientsByRecipeId(conn, tarifID);
        ingredientsListView.getItems().addAll(ingredients);

        // Malzeme eklemek için alan
        TextField ingredientField = new TextField();
        ingredientField.setPromptText("Yeni malzeme ekle");

        Button addIngredientButton = new Button("Malzeme Ekle");
        addIngredientButton.setOnAction(e -> {
            String newIngredient = ingredientField.getText().trim();
            if (!newIngredient.isEmpty()) {
                // Malzeme ID'sini almak için getOrCreateIngredient metodunu çağır
                int ingredientId = db.getOrCreateIngredient(conn, newIngredient);
                if (ingredientId != -1) { // Malzeme başarıyla alındı veya eklendi
                    ingredientsListView.getItems().add(newIngredient);
                    ingredientField.clear();
                }
            }
        });

        Button removeIngredientButton = new Button("Seçili Malzemeyi Sil");
        removeIngredientButton.setOnAction(e -> {
            String selectedIngredient = ingredientsListView.getSelectionModel().getSelectedItem();
            if (selectedIngredient != null) {
                ingredientsListView.getItems().remove(selectedIngredient);
                // Seçilen malzeme silindiğinde veritabanından silme işlemi yapılabilir
                db.deleteRecipe(conn, tarifID);
            }
        });

        HBox ingredientButtons = new HBox(10, ingredientField, addIngredientButton, removeIngredientButton);

        // Layout düzeni için VBox
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(
                new Label("Tarif Adı:"), nameField,
                new Label("Kategori:"), categoryComboBox,
                new Label("Hazırlama Süresi (dakika):"), preparationTimeField,
                new Label("Talimatlar:"), instructionsField,
                new Label("Malzemeler:"), ingredientsListView, ingredientButtons
        );
        dialog.getDialogPane().setContent(vbox);

        // Dialog'a butonlar ekle
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Kullanıcı 'OK' butonuna tıklarsa işlemi gerçekleştirme
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Kullanıcıdan alınan yeni veriler
                String newName = nameField.getText();
                String newCategory = categoryComboBox.getValue();
                int newPreparationTime = Integer.parseInt(preparationTimeField.getText());
                String newInstructions = instructionsField.getText();
                List<String> updatedIngredients = new ArrayList<>(ingredientsListView.getItems());

                // Veritabanında güncelleme yap
                db.updateRecipe(conn, tarifID, newName, newCategory, newPreparationTime, newInstructions);
                db.updateRecipeIngredients(conn, tarifID, updatedIngredients);

                // Güncelleme sonrası tarif listesini yeniden yükle
                loadRecipes(arama.getText());
            }
        });
    }




    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:./identifier.sqlite");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}