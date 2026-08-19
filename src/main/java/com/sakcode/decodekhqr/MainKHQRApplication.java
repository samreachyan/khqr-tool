package com.sakcode.decodekhqr;

import com.sakcode.decodekhqr.ui.KhqrFormController;
import com.sakcode.decodekhqr.ui.KhqrFormView;
import com.sakcode.decodekhqr.ui.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Taskbar;

public class MainKHQRApplication extends Application {

    private Theme currentTheme = Theme.LIGHT;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("KHQR Tool - @samreachyan");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon_64.png")));

        // Set macOS Dock icon when running outside a bundled .app (e.g. gradle run, java -jar)
        if (Taskbar.isTaskbarSupported()) {
            try {
                Taskbar taskbar = Taskbar.getTaskbar();
                java.awt.Image dockIcon = ImageIO.read(getClass().getResourceAsStream("/icon_64.png"));
                if (dockIcon != null) {
                    taskbar.setIconImage(dockIcon);
                }
            } catch (Exception e) {
                // Ignore: platform may not allow dock icon changes
            }
        }

        KhqrFormView.Layout layout = new KhqrFormView().build();
        new KhqrFormController(layout).wireActions();

        Scene scene = new Scene(layout.root(), 1200, 820);
        scene.getStylesheets().addAll(stylesheet(Theme.BASE_STYLESHEET), stylesheet(currentTheme.stylesheet()));

        layout.themeToggleButton().setText(currentTheme.opposite().toggleLabel());
        layout.themeToggleButton().setOnAction(e -> toggleTheme(scene, layout));

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();
    }

    private void toggleTheme(Scene scene, KhqrFormView.Layout layout) {
        scene.getStylesheets().remove(stylesheet(currentTheme.stylesheet()));
        currentTheme = currentTheme.opposite();
        scene.getStylesheets().add(stylesheet(currentTheme.stylesheet()));
        layout.themeToggleButton().setText(currentTheme.opposite().toggleLabel());
    }

    private String stylesheet(String path) {
        return getClass().getResource(path).toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}