package com.prime.ev.register.gui;

import com.prime.ev.register.Factory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public TextField nameField, otherField, surnameField, emailField, phoneField, occupationField;

    /*
    private class FieldListener implements ChangeListener<Boolean> {
        TextField textField;
        String assocLabel;

        FieldListener(TextField textField){
            this.textField = textField;
            this.textField.focusedProperty().addListener(this);
            assocLabel = "#" + textField.getId().replace("Field", "Label");
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldB, Boolean newB) {
            textField.getParent().lookup(assocLabel).setVisible(newB);
        }
    }
    */


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        List<TextField> fields = List.of(nameField, otherField, surnameField, emailField, phoneField, occupationField);
        //for(TextField textField: fields) new FieldListener(textField);
    }



    Factory.EventListener listener = new Factory.EventListener() {
        @Override
        public void onImageCaptured() {

        }

        @Override
        public void onUserRegistered() {

        }

        @Override
        public void onError() {

        }
    };

}
