package com.prime.ev.register.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.prime.ev.register.Factory;
import com.prime.net.forms.MultipartForm;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class LoginController {
    private final String loginURL = com.prime.ev.register.Factory.HOST + "/evoting_api/v1/admins/login";

    public TextField emailField;
    public PasswordField passwordField;

    public void login() throws Exception{
        HttpURLConnection http = (HttpURLConnection) new URL(loginURL).openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");

        http.setDoOutput(true);

        String boundary = "Prime'sBoundary";
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"");

        MultipartForm mpf = new MultipartForm(boundary, http.getOutputStream());
        mpf.addInput("adminEmail", emailField.getText());
        mpf.addInput("adminPassword", passwordField.getText());
        mpf.end();

        if(http.getResponseCode()== HttpURLConnection.HTTP_OK){
            String response = new String(new BufferedInputStream(http.getInputStream()).readAllBytes());
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map serverResponse = new Gson().fromJson(response, mapType);
            Factory.setX_access_token((String) serverResponse.get("token"));
            loginSuccessful();
        }
        else {
            System.out.println(http.getResponseCode());
            loginError();
        }
    }



    private void loginSuccessful() throws IOException { Factory.setRegistrationScene(); }



    private void loginError(){
        System.out.println("login failed");
    }
}
