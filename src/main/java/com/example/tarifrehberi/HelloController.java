package com.example.tarifrehberi;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable{
    @FXML
    private Label welcomeText;
    public Button deneme;
    public TextField arama;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
      //  deneme.setText("Yok et");
       // arama=new TextField();

        arama.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                arama.setFocusTraversable(false);
            }
        });

    }
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
    @FXML
    protected void onButon0ButtonClick(){welcomeText.setText("");}



}
