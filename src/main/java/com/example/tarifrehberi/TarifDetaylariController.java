package com.example.tarifrehberi;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TarifDetaylariController {
    @FXML
    private Label recipeNameLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label preparationTimeLabel;

    @FXML
    private Label tarif;

    public void initialize(String recipeName, String category, int preparationTime, String Tarif) {//Tarif detayları sayfası yüklendiğinde çağrılır. Gelen parametreleri ilgili etiketlere atar.

        recipeNameLabel.setText(recipeName);//Tarif adını etiket üzerine yazar.
        categoryLabel.setText("Kategori: " + category);
        preparationTimeLabel.setText("Hazırlama Süresi: " + preparationTime + " dakika");
        tarif.setText(Tarif);
        tarif.setWrapText(true);
    }
}