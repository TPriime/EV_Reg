package com.prime.ev.register;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.prime.net.forms.MultipartForm;

public class Factory{
    private static List<EventListener> listeners = new ArrayList<>();
    private static String ELECTION_REG_API = "http://127.0.0.1:4050/" + "evoting_api/v1/users/register";
    //private static String ELECTION_REG_API = "http://127.0.0.1:8080";

    public interface EventListener{
        void onImageCaptured();
        void onUserRegistered(String response);
        void onError();
    }


    private String formatFormInput(String boundary, String s ) {
        StringBuilder sb = new StringBuilder();

        return "";
    }



    public static List<Factory.EventListener> getListeners(){ return listeners;}


    public static void register(Map<String, String> userDetails) throws java.io.IOException{
        /*@debug*/ userDetails.forEach((s1, s2)-> System.out.println(s1 + " - " + s2));

        HttpURLConnection http = (HttpURLConnection) new URL(ELECTION_REG_API).openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("User-Agent", "Mozilla/5.0");
        String boundary = "Prime'sBoundary";
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"");//"application/json");
        http.setDoOutput(true);

        /*
        StringBuilder sb = new StringBuilder();
        userDetails.forEach((k, v)-> sb.append(","+k+"="+v));
         */

        OutputStream out = http.getOutputStream();
        //out.write(sb.toString().replaceFirst(",","").getBytes());
        //String jsonString = new Gson().toJson(userDetails);
        //out.write(jsonString.getBytes());
        //System.out.println(jsonString);

        MultipartForm mpf = new MultipartForm(boundary);
        userDetails.forEach((k, v)-> {
            if(k.equals("userProfilePicture")) mpf.addInputFile(k, (userDetails.get("userEmail")+".png"), v.getBytes());
            else mpf.addInput(k, v);
        });
        out.write(mpf.end().getBytes());
        out.close();

        //System.out.println(new String(new byte[]{1, 23, 4, 5, 12, 4}));

        final String[] response = {""};
        if(http.getResponseCode() == HttpURLConnection.HTTP_OK){
            response[0] = new String(new BufferedInputStream(http.getInputStream()).readAllBytes());
        } else { response[0] = Integer.toString(http.getResponseCode()); }

        listeners.forEach(l -> l.onUserRegistered(response[0]));

    }
}
