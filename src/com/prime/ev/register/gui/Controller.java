package com.prime.ev.register.gui;

import com.github.sarxos.webcam.Webcam;
import com.prime.ev.register.Factory;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Controller implements Initializable {
    private Map<String, Object> details = new HashMap<>();
    private ToggleGroup genderGroup = new ToggleGroup(), marriedStatusGroup = new ToggleGroup();
    private final int STATE = 0, LGA = 1, TOWN = 2;
    private Node parent;
    private boolean isCapureModeOn = false;
    private byte[] currentUserImageBytes;

    public TextField firstNameField, otherNamesField, lastNameField, userEmailField, phoneNumberField, occupationField;
    public RadioButton maleButton, femaleButton, singleButton, marriedButton;
    public ComboBox<String> state, lga, town;
    public DatePicker dateOfBirth;
    public ImageView userProfilePicture;


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
        Factory.EventListener listener = new Factory.EventListener() {
            @Override public void onImageCaptured(byte[] image) {
                isCapureModeOn = false;
                currentUserImageBytes = image;
                Platform.runLater(()->
                    userProfilePicture.setImage(new Image(new ByteArrayInputStream(image))));
            }

            @Override public void onUserRegistered(String response) {
                if(response.equalsIgnoreCase("error")) {
                    System.out.println("registration failed");
                    return;
                }

                System.out.println("user registered");
                System.out.println("response: "+response);
                cleanUp();
            }

            @Override public void onError() {

            }
        };
        Factory.getListeners().add(listener);

        try {
            state.setItems(getData(STATE, null));
        } catch (java.sql.SQLException sqle){
            System.out.println(sqle.getClass().getName()+": "+ sqle.getMessage());
            System.out.println("Unable to populate State ComboBox items");
            System.exit(1);
        }

        maleButton.setToggleGroup(genderGroup);
        femaleButton.setToggleGroup(genderGroup);
        singleButton.setToggleGroup(marriedStatusGroup);
        marriedButton.setToggleGroup(marriedStatusGroup);

        setListenersForComboBoxes();
        //initialize state with first item
        state.setValue(state.getItems().get(0));
    }


    private void setListenersForComboBoxes(){
        state.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue)->{
            try{
                lga.setItems(getData(LGA, newValue));
                lga.setValue(lga.getItems().get(0));
            }catch(java.sql.SQLException e){e.printStackTrace();}
        });

        lga.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue)->{
            try{
                town.setItems(getData(TOWN, newValue));
                town.setValue(town.getItems().get(0));
            }catch(java.sql.SQLException e){e.printStackTrace();}
        });
    }


    private ObservableList<String> getData(int value, String matchString) throws java.sql.SQLException{
        if(value == STATE) return FXCollections.observableArrayList(Factory.getStates());
        else if(value == LGA) return FXCollections.observableArrayList(Factory.getLGAs(matchString));
        else if(value == TOWN) return FXCollections.observableArrayList(Factory.getTowns(matchString));
        throw new IllegalArgumentException(String.format("value argument must be one of %s.STATE, %s.LGA or %s.TOWN", getClass().getSimpleName(),
                getClass().getSimpleName(), getClass().getSimpleName()));
    }


    private void cleanUp(){
        parent.lookupAll("TextField").forEach(t-> ((TextField)t).setText(""));
        //parent.lookupAll("ComboBox").forEach(c -> ((ComboBox<String>)c))
    }


    public void capture() {
        if(isCapureModeOn) Factory.captureImage();
        else {
            isCapureModeOn = true;
            Factory.beginCapture(userProfilePicture);
        }
    }


    public void register(){
        parent = firstNameField.getScene().getRoot();

        try{
            parent.lookupAll("TextField").forEach(t ->
                details.put(t.getId().replace("Field", ""), ((TextField)t).getText()));
            parent.lookupAll("ComboBox").forEach(c ->
                details.put(c.getId(),((ComboBox<String>)c).getValue()));

            RadioButton genderButton = (RadioButton)genderGroup.getSelectedToggle(),
                maritalButton =  (RadioButton) marriedStatusGroup.getSelectedToggle();
            details.put("gender", genderButton.getText());
            details.put("maritalStatus", maritalButton.getText());

            details.put(dateOfBirth.getId(), dateOfBirth.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));



            /////////////////////
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream f  = new FileInputStream(getClass().getResource("pic2.jpg").getFile());
            ImageIO.write(ImageIO.read(f), "jpg", bos);

            details.putAll(Map.of("fingerprint", "fingerprint",
                    "userID", "12345",
                    "userProfilePicture", currentUserImageBytes,//bos.toByteArray(),//new String(new byte[]{2,3,12,23,44,34,2}),
                    "profilePictureId", "24424343"));
            //////////////////////


            //*@debug*/ details.forEach((s1, s2)-> System.out.println(s1 + " - " + s2));
            Factory.register(details);

        } catch(NullPointerException npe){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Incomplete details");
            //alert.setHeaderText("DateE");
            //alert.setContentText("please fill the missing fields");
            alert.showAndWait();
            System.out.println("incomplete details");
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
