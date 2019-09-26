module EV.Reg {
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires java.desktop;
    requires gson;
    //requires webcam.capture;
    requires javafx.swing;
    //requires bridj;
    //requires slf4j.api;
    requires testcam;

    opens com.prime.ev.register.gui;
}