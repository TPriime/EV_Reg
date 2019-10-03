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
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private Map<String, Object> details = new HashMap<>();
    private ToggleGroup genderGroup = new ToggleGroup(), marriedStatusGroup = new ToggleGroup();
    private final int STATE = 0, LGA = 1, TOWN = 2;
    private Node parent;
    private boolean isCaptureModeOn = false;
    private byte[] currentUserImageBytes;
    private final long hideViewTimeout = Factory.promptTimeout;
    private final LocalDate defaultDate = LocalDate.of(1960, 10, 1);
    private final Image defaultImage = new Image(getClass().getResource("default.jpg").toExternalForm());

    public TextField firstNameField, otherNamesField, lastNameField, userEmailField, phoneNumberField, occupationField;
    public RadioButton maleButton, femaleButton, singleButton, marriedButton;
    public ComboBox<String> state, lga, town;
    public DatePicker dateOfBirth;
    public ImageView userProfilePicture;
    public Label registrationPrompt;

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
                imageCaptured(image);
            }

            @Override public void onRegister(String response) {
                if(response.equalsIgnoreCase("Registration successful")) userRegistered(response);
                else registrationFailed(response);
            }

            @Override public void onError(Exception e) {
                System.out.println(e.getClass().getSimpleName() +": "+e.getMessage());
                Platform.runLater(()->{
                    registrationPrompt.setText(e.getMessage());
                    registrationPrompt.setTextFill(Paint.valueOf("red"));
                });
                setHideView(registrationPrompt, hideViewTimeout);
            }

            @Override
            public void onDeviceDetected() {

            }

            @Override
            public void onDeviceDetached() {

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
        dateOfBirth.setValue(defaultDate);
        userProfilePicture.setImage(defaultImage);
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


    private void setHideView(Node node, long timeOutMillis){
        Platform.runLater(()->node.setVisible(true));
        new Thread(()->{
            try{Thread.sleep(timeOutMillis);}catch(Exception e){e.printStackTrace();}
            Platform.runLater(()->node.setVisible(false));
        }, "hideViewThread").start();
    }


    private void imageCaptured(byte[] image){
        isCaptureModeOn = false;
        currentUserImageBytes = image;
        Platform.runLater(()->
                userProfilePicture.setImage(new Image(new ByteArrayInputStream(image))));
    }


    private void registrationFailed(String errorMessage){
        System.out.println("error: " + errorMessage);
        Platform.runLater(()->{
            registrationPrompt.setText(errorMessage+"!");
            registrationPrompt.setTextFill(Paint.valueOf("red"));
        });
        setHideView(registrationPrompt,  hideViewTimeout);
    }


    private void userRegistered(String response) {
        System.out.println(response);
        Platform.runLater(()->{
            registrationPrompt.setText(response+"!");
            registrationPrompt.setTextFill(Paint.valueOf("green"));
        });
        setHideView(registrationPrompt, hideViewTimeout);
        //reset();             //////////////////////////////////////////////////////////////////////////////////
    }


    private void reset() {
        currentUserImageBytes = null;
        Platform.runLater(()->{
            parent.lookupAll("TextField").forEach(t-> ((TextField)t).setText(""));
            state.setValue(state.getItems().get(0));
            userProfilePicture.setImage(defaultImage);
            dateOfBirth.setValue(defaultDate);
        });
    }


    private List<String> gatherFingerprints(){
        return maleButton.getScene().getRoot().lookupAll("ImageView").stream()
                .map(node->node.getId())
                .filter(s -> s!=null)
                .filter(s->!(s.equals("currentFingerprintImage")||s.equals("userProfilePicture")))
                .collect(Collectors.toList());
    }


    public void capture() {
        if(isCaptureModeOn) Factory.captureImage();
        else {
            isCaptureModeOn = true;
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

            /*
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream f  = new FileInputStream(getClass().getResource("default.jpg").getFile());
            ImageIO.write(ImageIO.read(f), "jpg", bos);
             */


            details.putAll(Map.of(
                    "fingerprint", gatherFingerprints().toString(),//"fingerprint",
                    "userID", "12345",
                    "userProfilePicture", currentUserImageBytes,//bos.toByteArray()
                    "profilePictureId", "24424343"));


            //*@debug*/ details.forEach((s1, s2)-> System.out.println(s1 + " - " + s2));
            Factory.register(details);

        } catch(NullPointerException npe){
            System.out.println("incomplete details");
            Platform.runLater(()->{
                registrationPrompt.setText("incomplete details!");
                registrationPrompt.setTextFill(Paint.valueOf("red"));
            });
            setHideView(registrationPrompt, hideViewTimeout);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
