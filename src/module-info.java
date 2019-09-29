module EV.Reg {
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires java.desktop;
    requires gson;
    requires javafx.swing;
    requires webcam;
    requires PCardReader;
    requires java.smartcardio;

    opens com.prime.ev.register.gui;
}