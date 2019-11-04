package com.prime.ev.register.gui;

import com.prime.ev.register.Factory;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class FingerCaptureController {//implements Initializable {

    private Set<ImageView> fingerprintViews;
    private ImageView targetImage;
    private Scene scene;
    private Label registrationPrompt;
    private boolean isSceneSet; //scene accessible?
    private final Map<ImageView, String> fingerToLabelPair = new HashMap<>();

    public javafx.scene.image.ImageView currentFingerprintImage;
    public javafx.scene.control.Button captureButton;
    public javafx.scene.control.Label fingerLabel;

    public void initialize(){
        new Thread(()->{
            while (!isSceneSet) {
                try{ init(); }  //initialize
                catch(NullPointerException npe){} //thrown by calling scene.getRoot() when scene is null
                catch(Exception e){e.printStackTrace();}
            }
        }, "InitializeThread").start();
    }



    public void init(){
        scene = currentFingerprintImage.getScene();  //returns null if called too early, since render isn't complete
        fingerprintViews = scene.getRoot().lookupAll("ImageView").stream()  //note: a loop occurs and stops here while an NullPointerException is thrown
                .filter(node->{
                    if(node.getId()!=null)
                        return !(node.getId().equals("currentFingerprintImage") || node.getId().equals("userProfilePicture"));
                    return true;
                })
                .map(node->(ImageView)node)
                .collect(Collectors.toUnmodifiableSet());
        fingerprintViews.forEach(imageView -> imageView.setOnMouseClicked(e->setTargetFingerImage(imageView)));
        registrationPrompt = ((Label)scene.lookup("#registrationPrompt"));
        isSceneSet = true;

        if(fingerToLabelPair.size()==0) setFingerToLabelPair();

        Factory.getListeners().add(new Factory.EventListener() {
            @Override public void onImageCaptured(byte[] image) { }
            @Override public void onFingerprintCaptured(Map<String, Object> fingerMap){ _onFingerprintCaptured(fingerMap);}
            @Override public void onRegister(String response) {reset();}
            @Override public void onError(Exception e) { }
            @Override public void onDeviceDetected() { }
            @Override public void onDeviceDetached() { }
        });
    }


    private void reset(){
        fingerprintViews.forEach(imView->{
            imView.setImage(
                    new Image(getClass().getResource("default_fp.jpg").toExternalForm()));
            imView.setId("");
            imView.setOpacity(0.4);
        });
    }


    private void setTargetFingerImage(ImageView imView){
        targetImage = imView;
        fingerLabel.setText(fingerToLabelPair.get(targetImage));
    }


    private void setHideView(Node node, long timeOutMillis){
        Platform.runLater(()->node.setVisible(true));
        new Thread(()->{
            try{Thread.sleep(timeOutMillis);}catch(Exception e){e.printStackTrace();}
            Platform.runLater(()->node.setVisible(false));
        }, "hideViewThread").start();
    }


    private void setFingerToLabelPair(){
        fingerprintViews.forEach(imageView ->
                fingerToLabelPair.put(imageView, imageView.getId()
                        .replace("_", " ")
                        .replace("1", "(")
                        .replace("2", ")")));
    }


    public void capture(){
        if(targetImage == null) {
            registrationPrompt.setText("select a finger");
            registrationPrompt.setTextFill(Paint.valueOf("red"));
            setHideView(registrationPrompt, Factory.promptTimeout);
            return;
        }

        Factory.captureFingerprint();
    }


    private void _onFingerprintCaptured(Map<String, Object> fingerMap){
        Platform.runLater(()->{
            targetImage.setImage((Image) fingerMap.get("fingerprintImage"));
            targetImage.setId((String)fingerMap.get("fingerprint"));
            targetImage.setOpacity(1);
            targetImage = null;
            fingerLabel.setText("Click on a finger");
        });
    }
}
