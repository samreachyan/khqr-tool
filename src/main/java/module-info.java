module com.ftb.decodekhqr {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires sdk.java;
    requires com.google.zxing;
    requires java.desktop;
    requires com.google.zxing.javase;
    requires com.fasterxml.jackson.databind;

    opens com.sakcode.decodekhqr to javafx.fxml;
    exports com.sakcode.decodekhqr;
}