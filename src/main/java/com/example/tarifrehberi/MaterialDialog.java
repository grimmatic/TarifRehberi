package com.example.tarifrehberi;

import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.image.Image;

import java.sql.Connection;

public class MaterialDialog {
    private Runnable onMaterialAddedCallback;
    private final Database database;

    public MaterialDialog() {
        this.database = new Database(); // Database sınıfını başlat
    }

    public void showAddMaterialDialog() {
        // Dialog penceresi oluşturma
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Malzeme Ekle");

        // Header için gerekli bileşenleri oluştur
        Label headerLabel = new Label("Yeni malzeme eklemek için bilgileri girin:");
        headerLabel.setStyle("-fx-background-color: #7a1e9c; -fx-text-fill: white; -fx-padding: 10; -fx-font-size: 16px;");
        headerLabel.setAlignment(Pos.CENTER);

        Image image = new Image(getClass().getResourceAsStream("/icons/MalzemeEkle.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.getChildren().addAll(headerLabel, spacer, imageView);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #7a1e9c; -fx-padding: 10;");
        header.setMaxWidth(Double.MAX_VALUE);

        // Dialog penceresine header'ı ekle
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setHeader(header);
        dialogPane.setStyle("-fx-background-color: #efe3f2;");

        // Dialog için bir GridPane oluştur
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Malzeme adı girişi için bir Label ve TextField ekle
        Label materialNameLabel = new Label("Malzeme Adı:");
        TextField materialNameField = new TextField();
        materialNameField.setPromptText("Malzeme Adı");
        materialNameField.setPrefWidth(400); // Dialog penceresi ile eşit genişlikte
        grid.add(materialNameLabel, 0, 0);
        grid.add(materialNameField, 0, 1);

        // Malzeme miktarı girişi için bir Label ve TextField ekle
        Label materialQuantityLabel = new Label("Toplam Miktar:");
        TextField materialQuantityField = new TextField();
        materialQuantityField.setPromptText("Toplam Miktar");
        materialQuantityField.setPrefWidth(400); // Dialog penceresi ile eşit genişlikte
        grid.add(materialQuantityLabel, 0, 2);
        grid.add(materialQuantityField, 0, 3);

        // Malzeme birimi için bir Label ve ComboBox ekleyin
        Label materialUnitLabel = new Label("Birim Seçin:");
        ComboBox<String> materialUnitComboBox = new ComboBox<>(); // ComboBox oluştur
        materialUnitComboBox.getItems().addAll("kilogram", "litre", "adet"); // Birimleri ekleyin
        materialUnitComboBox.setPromptText("Birim Seçin"); // Kullanıcıya ne yapması gerektiğini bildirin
        materialUnitComboBox.setPrefWidth(400);
        grid.add(materialUnitLabel, 0, 4);
        grid.add(materialUnitComboBox, 0, 5);

        // Malzeme birim fiyatı için bir Label ve TextField ekle
        Label materialUnitPriceLabel = new Label("Birim Fiyat:");
        TextField materialUnitPriceField = new TextField();
        materialUnitPriceField.setPromptText("Birim Fiyat");
        materialUnitPriceField.setPrefWidth(400); // Dialog penceresi ile eşit genişlikte
        grid.add(materialUnitPriceLabel, 0, 6);
        grid.add(materialUnitPriceField, 0, 7);

        // Mesaj gösterimi için bir Label ekleyin
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;"); // Hata mesajı için stil
        grid.add(messageLabel, 0, 8); // GridPane'in altına ekleyin

        // Dialog penceresine GridPane ekle
        dialogPane.setContent(grid);

        // Ekle butonunu ayarla
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("İptal");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Malzeme Ekle");
        // Dialog'u göster
        dialog.show();

        // Buton kontrolü
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Kullanıcı "OK" butonuna bastı, malzeme verilerini al
            String materialName = materialNameField.getText().trim(); // Trim ekleyerek boşlukları kaldırdık
            String materialQuantity = materialQuantityField.getText().trim();
            String materialUnit = materialUnitComboBox.getValue(); // ComboBox'dan birimi al
            String materialUnitPrice = materialUnitPriceField.getText().trim();

            // Kontroller
            if (materialName.isEmpty()) {
                messageLabel.setText("Malzeme adı boş olamaz."); // Hata mesajını göster
                event.consume(); // Dialog kapanmasın
                return; // Dialog kapanmasın
            }
            if (materialQuantity.isEmpty()) {
                messageLabel.setText("Toplam miktar boş olamaz."); // Hata mesajını göster
                event.consume(); // Dialog kapanmasın
                return; // Dialog kapanmasın
            }
            if (materialUnit == null || materialUnit.isEmpty()) {
                messageLabel.setText("Birim alanı boş olamaz."); // Hata mesajını göster
                event.consume(); // Dialog kapanmasın
                return; // Dialog kapanmasın
            }
            if (materialUnitPrice.isEmpty()) {
                messageLabel.setText("Birim fiyatı boş olamaz."); // Hata mesajını göster
                event.consume(); // Dialog kapanmasın
                return; // Dialog kapanmasın
            }

            try {
                // Toplam Miktar ve Birim Fiyat için varsayılan değerleri kontrol et
                Integer quantity = parseQuantity(materialQuantity);
                Double unitPrice = parseUnitPrice(materialUnitPrice);

                // Veritabanı bağlantısı oluştur
                try (Connection conn = database.connect()) {
                    if (conn != null) {
                        // Malzeme ekleme işlemini gerçekleştirin
                        database.addMalzeme(conn, materialName, materialQuantity, unitPrice, materialUnit);
                        if (onMaterialAddedCallback != null) {
                            onMaterialAddedCallback.run();
                        }
                        dialog.close(); // Dialog kapanmasını sağlar
                    }
                }
            } catch (RuntimeException e) {
                messageLabel.setText(e.getMessage()); // Hata mesajını göster
                event.consume(); // Dialog kapanmasın
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setText("Malzeme eklenirken bir hata oluştu: " + e.getMessage());
                event.consume(); // Dialog kapanmasın
            }
        });




    }

    private Integer parseQuantity(String materialQuantity) {
        // Toplam Miktar için kontrol
        if (materialQuantity == null || materialQuantity.trim().isEmpty()) {
            return 1; // Varsayılan değer
        }
        int quantity = Integer.parseInt(materialQuantity);
        return Math.max(quantity, 1); // Negatif değerler için en az 1
    }

    private Double parseUnitPrice(String materialUnitPrice) {
        // Birim Fiyat için kontrol
        if (materialUnitPrice == null || materialUnitPrice.trim().isEmpty()) {
            return 10.0; // Varsayılan değer
        }
        double unitPrice = Double.parseDouble(materialUnitPrice);
        return Math.max(unitPrice, 10.0); // Negatif değerler için en az 10.0
}
public void setOnMaterialAddedCallback(Runnable callback) {
    this.onMaterialAddedCallback = callback;
}}