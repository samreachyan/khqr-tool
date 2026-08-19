package com.sakcode.decodekhqr.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

/**
 * Dialog that exports an A4 sheet (300 DPI) filled with copies of the KHQR card in a grid layout.
 */
public final class PrintSheetDialog {

    private static final double A4_WIDTH = 2480;
    private static final double A4_HEIGHT = 3508;
    private static final double MARGIN = 120;
    private static final double GAP = 40;

    private final Image cardImage;

    public PrintSheetDialog(Image cardImage) {
        this.cardImage = cardImage;
    }

    public void show(Window owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Print Sheet Export");
        dialog.setResizable(false);

        Label layoutLabel = new Label("Layout:");
        layoutLabel.getStyleClass().add("field-label");
        ComboBox<String> layoutCombo = new ComboBox<>();
        layoutCombo.getItems().addAll("1 x 1", "2 x 2", "2 x 3", "3 x 4");
        layoutCombo.setValue("2 x 2");
        layoutCombo.setMaxWidth(Double.MAX_VALUE);

        Label sheetsLabel = new Label("Sheets:");
        sheetsLabel.getStyleClass().add("field-label");
        Spinner<Integer> sheetsSpinner = new Spinner<>(1, 50, 1);
        sheetsSpinner.setEditable(true);
        sheetsSpinner.setMaxWidth(Double.MAX_VALUE);

        Button exportButton = new Button("Export PNG");
        exportButton.getStyleClass().add("primary-button");
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        HBox buttonRow = new HBox(10, cancelButton, exportButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, layoutLabel, layoutCombo, sheetsLabel, sheetsSpinner, buttonRow);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("tab-content");
        root.setPrefWidth(280);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource(Theme.BASE_STYLESHEET).toExternalForm(),
                getClass().getResource(Theme.LIGHT.stylesheet()).toExternalForm()
        );
        dialog.setScene(scene);

        exportButton.setOnAction(e -> {
            int[] grid = parseGrid(layoutCombo.getValue());
            int sheets = sheetsSpinner.getValue();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName("khqr-print-sheet.png");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
            File file = fileChooser.showSaveDialog(dialog);
            if (file != null) {
                try {
                    WritableImage sheet = renderSheet(grid[0], grid[1], sheets);
                    com.sakcode.decodekhqr.qr.PngImageWriter.write(sheet, file);
                    dialog.close();
                } catch (Exception ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
                    alert.initOwner(dialog);
                    alert.setTitle("Export Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to save sheet: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        cancelButton.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    private int[] parseGrid(String value) {
        String[] parts = value.split(" x ");
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    private WritableImage renderSheet(int cols, int rows, int sheets) {
        double usableW = A4_WIDTH - 2 * MARGIN;
        double usableH = A4_HEIGHT - 2 * MARGIN;
        double gapTotalW = (cols - 1) * GAP;
        double gapTotalH = (rows - 1) * GAP;

        double cardW = (usableW - gapTotalW) / cols;
        double cardH = cardW * (29.0 / 20.0);
        double totalH = rows * cardH + gapTotalH;

        if (totalH > usableH) {
            double scale = usableH / totalH;
            cardW *= scale;
            cardH *= scale;
        }

        double sheetW = A4_WIDTH;
        double sheetH = A4_HEIGHT * sheets;

        Canvas canvas = new Canvas(sheetW, sheetH);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, sheetW, sheetH);

        for (int s = 0; s < sheets; s++) {
            double offsetY = s * A4_HEIGHT;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    double x = MARGIN + c * (cardW + GAP);
                    double y = MARGIN + offsetY + r * (cardH + GAP);
                    gc.drawImage(cardImage, x, y, cardW, cardH);

                    // Cut-line guides
                    gc.setStroke(Color.web("#C9C9C9"));
                    gc.setLineWidth(1);
                    gc.setLineDashes(6, 5);
                    gc.strokeRect(x - 2, y - 2, cardW + 4, cardH + 4);
                    gc.setLineDashes();
                }
            }
            // Page separator line between sheets (except after last)
            if (s < sheets - 1) {
                gc.setStroke(Color.web("#999999"));
                gc.setLineWidth(2);
                gc.strokeLine(0, offsetY + A4_HEIGHT, sheetW, offsetY + A4_HEIGHT);
            }
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        return canvas.snapshot(params, new WritableImage((int) sheetW, (int) sheetH));
    }
}
