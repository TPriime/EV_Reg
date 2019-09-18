package com.prime.ev.register;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Factory{
    private static List<EventListener> listeners = new ArrayList<>();
    private static String ELECTION_REG_API = "http://127.0.0.1:5041/" + "evoting_api/v1/users/register";

    public interface EventListener{
        void onImageCaptured();
        void onUserRegistered(String response);
        void onError();
    }


    public static List<Factory.EventListener> getListeners(){ return listeners;}


    public static void register(Map<String, String> userDetails) throws java.io.IOException{
        /*@debug*/ userDetails.forEach((s1, s2)-> System.out.println(s1 + " - " + s2));

        HttpURLConnection http = (HttpURLConnection) new URL(ELECTION_REG_API).openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("User-Agent", "Mozilla/5.0");
        http.setDoOutput(true);

        StringBuilder sb = new StringBuilder();
        userDetails.forEach((k, v)-> sb.append(","+k+"="+v));

        OutputStream out = http.getOutputStream();
        out.write(sb.toString().replaceFirst(",","").getBytes());

        final String[] response = {""};
        if(http.getResponseCode() == HttpURLConnection.HTTP_OK){
            response[0] = new BufferedInputStream(http.getInputStream()).readAllBytes().toString();
        } else { response[0] = Integer.toString(http.getResponseCode()); }

        listeners.forEach(l -> l.onUserRegistered(response[0]));
    }
}
