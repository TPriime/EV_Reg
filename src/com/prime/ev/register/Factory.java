package com.prime.ev.register;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.prime.ev.register.gui.Main;
import com.prime.net.forms.MultipartForm;

public class Factory{
    public static final String HOST = "http://127.0.0.1:8080";

    private static Main mainInstance;
    private static List<EventListener> listeners = new ArrayList<>();
    private static String ELECTION_REG_API = HOST + "/evoting_api/v1/users/register";
    //private static String ELECTION_REG_API = "http://127.0.0.1:8080";
    private static Connection conn;
    private static String x_access_token = "";//"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwYXlsb2FkIjp7ImlkIjoiNWQ1ZTY0NjQzODdjODI3MmViNDdhNmEzIn0sImlhdCI6MTU2NjQ2NzI4NSwiZXhwIjoxNTY5MDU5Mjg1fQ.JNw0G7mcOHB1EJdEGfu8mdrrW-6-41SnloIy2sXWbPA";


    public interface EventListener{
        void onImageCaptured();
        void onUserRegistered(String response);
        void onError();
    }



    static {
        try{
            createDBConnection();
            initializeDB();
        } catch(Exception e){
            System.out.println(e.getClass().getName()+": "+e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


    private static void createDBConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:location.db");
        conn.setAutoCommit(false);
    }


    private static void initializeDB() throws SQLException{
        Statement stmt = conn.createStatement();
        //check if any of the States or LGAs tables exist, create a new table for each otherwise
        ResultSet rs = stmt.executeQuery("SELECT * FROM (SELECT * FROM sqlite_master where type='table') where name='States' or name='LGAs'");
        if(!rs.next()){
            System.out.println("No table found! ");
            stmt.executeUpdate("CREATE TABLE States(name CHAR(25) PRIMARY KEY)");
            stmt.executeUpdate("CREATE TABLE LGAs(name CHAR(25) PRIMARY KEY, state CHAR(25) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE Towns(name CHAR(25) PRIMARY KEY, lga CHAR(25) NOT NULL)");

            /*@debug*/
            for(int x=0; x<100; x++){
                stmt.executeUpdate("INSERT INTO States VALUES('"+x+"')");
                int inc = x*20;
                for(int i=(500+inc); i<(500+inc+20); i++ ){
                    stmt.executeUpdate(String.format("INSERT INTO LGAs VALUES('%d', '%d')", i, x));
                    int incc = (i-500)*50;
                    for(int j=(1000+incc); j<(1000+incc+50); j++)
                        stmt.executeUpdate(String.format("INSERT INTO Towns VALUES('%d', '%d')", j, i));
                }
            }
            //conn.commit();
            /*@debug*/
            System.out.println("Tables created");
        }

        /*@debug*/
        /*System.out.println("----States----");
        rs = stmt.executeQuery("Select * from States");
        while(rs.next()) System.out.println(rs.getString("name"));

        System.out.println();

        System.out.println("----LGAs----");
        rs = stmt.executeQuery("Select * from LGAs");
        while(rs.next()) System.out.println(rs.getString("name") + "  " + rs.getString("state"));

        System.out.println("----Towns----");
        rs = stmt.executeQuery("Select * from Towns");
        while(rs.next()) System.out.println(rs.getString("name") + "  " + rs.getString("lga"));*/
        /*@debug*/
    }


    public static void setX_access_token(String x_access_token){
        Factory.x_access_token = x_access_token;
    }


    public static String getX_access_token(){ return x_access_token; }


    public static void setMainInstance(Main main){ mainInstance = main; }


    public static void setRegistrationScene()throws IOException {mainInstance.setRegistrationScene();}


    public static List<String> getStates() throws SQLException{
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("Select * from States");
        ArrayList<String> stateList = new ArrayList<>();
        while(rs.next()) stateList.add(rs.getString("name"));
        return stateList;
    }


    public static List<String> getLGAs(String state) throws SQLException{
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("Select * from LGAs WHERE state='"+state+"'");
        ArrayList<String> lgaList = new ArrayList<>();
        while(rs.next()) lgaList.add(rs.getString("name"));
        return lgaList;
    }


    public static List<String> getTowns(String lga) throws SQLException{
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("Select * from Towns WHERE lga='"+lga+"'");
        ArrayList<String> townList = new ArrayList<>();
        while(rs.next()) townList.add(rs.getString("name"));
        return townList;
    }


    public static List<Factory.EventListener> getListeners(){ return listeners;}


    public static void register(Map<String, Object> userDetails) throws java.io.IOException{
        HttpURLConnection http = (HttpURLConnection) new URL(ELECTION_REG_API).openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("User-Agent", "Mozilla/5.0");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("x-access-token", x_access_token);

        String boundary = "Prime'sBoundary";
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"");
        http.setDoOutput(true);

        OutputStream out = http.getOutputStream();

        MultipartForm mpf = new MultipartForm(boundary, out);
        userDetails.forEach((k, v)-> {
            try{
                if(k.equals("userProfilePicture")) {
                    mpf.addInputFile(k, (userDetails.get("userEmail")+".png"), (byte[])v);
                }
                else mpf.addInput(k, (String)v);
            } catch(IOException ioe){ioe.printStackTrace();}
        });
        mpf.end();

        final String[] response = {""};
        if(http.getResponseCode() == HttpURLConnection.HTTP_OK){
            response[0] = new String(new BufferedInputStream(http.getInputStream()).readAllBytes());
            listeners.forEach(l -> l.onUserRegistered(response[0]));
        } else {
            response[0] = Integer.toString(http.getResponseCode());
            /*@debug*/System.out.println("response code: " + http.getResponseCode() + "; response msg: " + http.getResponseMessage());

            listeners.forEach(l -> l.onUserRegistered("error"));
        }
    }
}
