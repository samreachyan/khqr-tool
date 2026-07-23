package com.sakcode.decodekhqr;

import com.sakcode.decodekhqr.ui.KhqrFormController;
import com.sakcode.decodekhqr.ui.KhqrFormView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainKHQRApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("QR Code Generator and Decoder - @samreachyan");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon_64.png")));

        KhqrFormView.Layout layout = new KhqrFormView().build();
        new KhqrFormController(layout).wireActions();

        primaryStage.setScene(new Scene(layout.root(), 1000, 800));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}