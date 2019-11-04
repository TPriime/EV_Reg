package com.prime.ev.register;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import com.prime.ev.register.gui.Main;
import com.prime.net.forms.MultipartForm;
import com.prime.util.cardio.CardIO;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.smartcardio.*;


public class Factory{
    public static final String HOST = "http://127.0.0.1:8080";

    private static Webcam webcam;
    private static Thread imageViewThread;
    private static long frequency = 60;
    private static final List<Thread> threadList = new ArrayList<>();

    private static Main mainInstance;
    private static List<EventListener> listeners = new ArrayList<>();
    private static String ELECTION_REG_API = HOST + "/evoting_api/v1/users/register";
    //private static String ELECTION_REG_API = "http://127.0.0.1:8080";
    private static Connection conn;
    private static String x_access_token = "";
    private static CardTerminal operatingCardReaderDevice;
    private static Card card;



    public static long promptTimeout = 5000;

    public interface EventListener{
        void onImageCaptured(byte[] image);
        void onFingerprintCaptured(Map<String, Object> fingerprintMap);
        void onRegister(String response);
        void onError(Exception e);
        void onDeviceDetected();
        void onDeviceDetached();
    }


    static {
        try{
            createDBConnection();
            initializeDB();
            initializeCardReaderInterface();
        } catch(Exception e){
            System.out.println(e.getClass().getName()+": "+e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        webcam = Webcam.getDefault();
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


    private static void initializeCardReaderInterface(){
        CardIO.getInstance().addListener(new CardIO.ICardListener() {
            @Override
            public void onCardInserted(CardTerminal cardTerminal) {
                Factory.onCardInserted(cardTerminal);
            }

            @Override
            public void onCardEjected(CardTerminal cardTerminal) {
                Factory.onCardEjected(cardTerminal);
            }

            @Override
            public void onDeviceDetected(List<CardTerminal> list) {
                Factory.onDeviceDetected(list);
            }

            @Override
            public void onDeviceDetached(CardTerminal cardTerminal) {
                Factory.onDeviceDetached(cardTerminal);
            }
        });
    }


    private  static void onDeviceDetected(List<CardTerminal> cardTerminals) {
        if(operatingCardReaderDevice != null) return;
        operatingCardReaderDevice = cardTerminals.get(0);
        listeners.forEach(l->l.onDeviceDetected());
        System.out.println("device connected");
    }


    private static void onDeviceDetached(CardTerminal cardTerminal) {
        if(cardTerminal != operatingCardReaderDevice) return;
        operatingCardReaderDevice = null;
        listeners.forEach(l->l.onDeviceDetached());
        System.out.println("device detached");
    }


    private static void onCardInserted(CardTerminal cardTerminal){
        try{
            if(cardTerminal == operatingCardReaderDevice) {
                card = cardTerminal.connect("*");
                System.out.println("card connected");
            }
        } catch(CardException ce){ce.printStackTrace();}
    }


    private static void onCardEjected(CardTerminal cardTerminal) {
        if(cardTerminal == operatingCardReaderDevice) {
            card = null;
            System.out.println("card disconnected");
        }
    }


    private static boolean isNewCard(CardChannel channel) throws CardException, IOException{
        byte[] read_command = new byte[]{(byte)0xFF, (byte)0xB0, (byte)0x01, (byte)0x04, (byte)0x0A};
        ResponseAPDU response = channel.transmit(new CommandAPDU(read_command));
        /*
        HttpURLConnection http = (HttpURLConnection) new URL("").openConnection();
        http.setRequestMethod("GET");
        http.setRequestProperty("User-Agent", "Mozilla/5.0");
        if(http.getResponseCode() == HttpURLConnection.HTTP_OK){
            byte[] serverResponse = new BufferedInputStream(http.getInputStream()).readAllBytes();
            //return true or false
        } else {
            throw new IOException("server error - couldn't verify card");
        }
         */
        String testData = "I am Primc";
        if(new String(response.getData()).equals(testData)) return false;
        else return true;
    }



    private static byte[] generateCardId(){
        byte[] cardID = new byte[20];
        new Random().nextBytes(cardID);

        return (new String(cardID)+"I am Prime").getBytes();
    }


    private static String writeToCard()throws IOException, CardException{
        if(card == null) throw new IOException("no card present");

        CardChannel channel = card.getBasicChannel();
        if(!isNewCard(channel))  throw new CardException("Card has been used");

        byte[] verify_command = {(byte)0xFF, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0xFF, (byte)0xFF};
        ResponseAPDU response = channel.transmit(new CommandAPDU(verify_command));
        if(!(response.getSW1()==144 && response.getSW2()==255)) throw new CardException("Verify Error");

        byte[] commandData = generateCardId();
        byte[] write_command = {(byte)0xFF, (byte)0xD0, (byte)0x01, (byte)0x04, (byte)commandData.length};
        byte[] t = new byte[write_command.length + commandData.length];
        for (int i = 0; i < t.length; i++)
            if(i<write_command.length) t[i] = write_command[i];

        response = channel.transmit(new CommandAPDU(write_command));
        if(!(response.getSW1()==144 && response.getSW2()==0)) throw new CardException("Write Error");

        //card.disconnect(false);
        return new String(commandData);
    }


    private static void _register(Map<String, Object> userDetails) throws java.io.IOException, CardException{
        userDetails.put("cardID", writeToCard()); //add cardID

        /*@debug*/userDetails.forEach((s, o)->System.out.printf("%s: %s\n", s, o));////////////////////////////////////////////////////////

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
            System.out.println("server response: "+response[0]);
            response[0] = "Registration successful";
        }
        else if (http.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) //400
            response[0] = "Email or Phone already exists";
        else if(http.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) //401
            response[0] = "incomplete details";
        else {
            response[0] = Integer.toString(http.getResponseCode());
            /*@debug*/System.out.println("response code: " + http.getResponseCode() + "; response msg: " + http.getResponseMessage());
            response[0] = "Registration failed";
        }
        listeners.forEach(l -> l.onRegister(response[0]));
    }


    private static void startThread(Thread task){
        task.start();
        threadList.add(task);
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


    public static void beginCapture(ImageView imgView){
        webcam.open();
        final Image[] img = new Image[1];
        imageViewThread = new Thread(()->{
            while(true){
                try {
                    img[0] = SwingFXUtils.toFXImage(webcam.getImage(), null);
                    Platform.runLater(()-> imgView.setImage(img[0]));
                    Thread.sleep((1/frequency)*1000);
                }
                catch (NullPointerException npe){
                    System.out.printf("%s : %s; webcam is probably null\n", npe.getClass().getName(), npe.getMessage());
                    break;
                }
                catch (Exception e){e.printStackTrace();}
            }
        }, "BeginCaptureThread");
        startThread(imageViewThread);
    }


    public static void captureImage(){
        imageViewThread.interrupt();
        byte[] imageBytes = WebcamUtils.getImageBytes(webcam, "jpg");
        webcam.close();

        listeners.forEach(l->l.onImageCaptured(imageBytes));
    }


    private static Map<String, Object> _captureFingerprint() throws IOException{
        //String fingerprintHash = "d123f2e323a";////////////////////////////////////////////////
        Process process = Runtime.getRuntime().exec("fingerprint.exe");
        BufferedInputStream buff  = new BufferedInputStream(process.getInputStream());
        String fingerCharacteristics = new String(buff.readAllBytes());
        if(fingerCharacteristics.equals("-1")) throw new IOException("no fingerprint reader!");
        else if(fingerCharacteristics.equals("-2")) throw new IOException("retry finger");

        //*@debug*/System.out.println("characteristics: " + fingerCharacteristics);

        return Map.of("fingerprint", fingerCharacteristics,
                "fingerprintImage", new Image(Factory.class.getResource("default_fp.jpg").toExternalForm()));
    }


    public static void captureFingerprint() {
        new Thread(()->{
            try {
                Map<String, Object> fingerprintMap = _captureFingerprint();
                listeners.forEach(l -> l.onFingerprintCaptured(fingerprintMap));
            }catch(Exception e){
                listeners.forEach(l->l.onError(e)); //listeners should handle exceptions
            }
        }).start();
    }

    /*
    public static List<String> getFingerprints(){ return fingerprints; }


    public static void clearFingerprints
*/

    public static void register(Map<String, Object> userDetails){
        Thread registerThread = new Thread(()->{
           try{ _register(userDetails); }
           catch(CardException ce){ listeners.forEach(l->l.onError(ce)); }
           catch(IOException ioe){ listeners.forEach(l->l.onError(ioe));}
        }, "RegisterThread");
        startThread(registerThread);
    }


    public static void killThreads(){
        threadList.forEach(Thread::interrupt);
    }
}
