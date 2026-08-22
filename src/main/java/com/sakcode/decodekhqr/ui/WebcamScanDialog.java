package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.qr.QrImageCodec;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modal dialog that opens the default webcam, shows a live preview, and auto-detects QR codes.
 * Returns the decoded QR string when found, or empty if cancelled.
 */
public final class WebcamScanDialog {

    static {
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Exception e) {
            System.err.println("OpenCV native library load failed: " + e.getMessage());
        }
    }

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private static final int DECODE_INTERVAL = 8; // attempt decode every N frames

    private final QrImageCodec qrImageCodec = new QrImageCodec();
    private final AtomicReference<String> result = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Optional<String> showAndWait(Window owner) {
        VideoCapture capture = new VideoCapture();
        if (!capture.open(0)) {
            showErrorAlert(owner, "Unable to open camera.\n" + cameraPermissionHint());
            return Optional.empty();
        }

        // Give the camera a moment to warm up
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Scan QR from Camera");
        dialog.setResizable(false);

        ImageView preview = new ImageView();
        preview.setFitWidth(PREVIEW_WIDTH);
        preview.setFitHeight(PREVIEW_HEIGHT);
        preview.setPreserveRatio(true);
        preview.getStyleClass().add("webcam-preview");

        Label statusLabel = new Label("Point camera at a QR code...");
        statusLabel.getStyleClass().add("status-label");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> {
            running.set(false);
            dialog.close();
        });

        VBox root = new VBox(12, preview, statusLabel, cancelButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("tab-content");

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource(Theme.BASE_STYLESHEET).toExternalForm(),
                getClass().getResource(Theme.LIGHT.stylesheet()).toExternalForm()
        );
        dialog.setScene(scene);

        running.set(true);
        AtomicInteger frameCount = new AtomicInteger(0);
        Mat frame = new Mat();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running.get()) {
                    stop();
                    return;
                }
                if (!capture.read(frame) || frame.empty()) {
                    return;
                }

                WritableImage image = matToWritableImage(frame);
                preview.setImage(image);

                int count = frameCount.incrementAndGet();
                if (count % DECODE_INTERVAL == 0) {
                    try {
                        String decoded = qrImageCodec.decode(image);
                        if (decoded != null && !decoded.isBlank()) {
                            result.set(decoded);
                            running.set(false);
                            stop();
                            dialog.close();
                        }
                    } catch (Exception ignored) {
                        // No QR found in this frame
                    }
                }
            }
        };

        dialog.setOnCloseRequest(e -> running.set(false));
        dialog.setOnShown(e -> timer.start());
        dialog.showAndWait();

        timer.stop();
        capture.release();
        frame.release();

        return Optional.ofNullable(result.get());
    }

    private static WritableImage matToWritableImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();

        byte[] buffer = new byte[width * height * channels];
        mat.get(0, 0, buffer);

        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int b = buffer[i * channels] & 0xff;
            int g = buffer[i * channels + 1] & 0xff;
            int r = buffer[i * channels + 2] & 0xff;
            pixels[i] = (0xff << 24) | (r << 16) | (g << 8) | b;
        }
        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
    }

    /**
     * The camera device can fail to open for different reasons per OS, so points the user at
     * the right fix: macOS gates it behind a Privacy setting, Linux behind {@code video} group
     * membership on {@code /dev/video0} (there's no OS permission prompt to grant on Linux).
     */
    private static String cameraPermissionHint() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return "Check permissions in System Settings > Privacy & Security > Camera.";
        }
        if (os.contains("linux")) {
            return "Your user may not have access to the camera device. Try:\n"
                    + "sudo usermod -aG video $USER\n"
                    + "then log out and back in, and check no other app is using the camera.";
        }
        return "Check that no other application is using the camera and that access permissions allow it.";
    }

    private void showErrorAlert(Window owner, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Camera Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
