package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.khqr.KhqrFormMapper;
import com.sakcode.decodekhqr.khqr.KhqrService;
import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import com.sakcode.decodekhqr.qr.PngImageWriter;
import com.sakcode.decodekhqr.qr.QrImageCodec;
import com.sakcode.decodekhqr.util.Timestamps;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/** Wires the form's buttons to {@link KhqrService}/{@link QrImageCodec} and reflects results back onto the UI. */
public final class KhqrFormController {

    /** Fixed output size for "Save Image" — the card is scaled up and centered to exactly fill this. */
    private static final double EXPORT_WIDTH = 750;
    private static final double EXPORT_HEIGHT = 1200;

    /** QR resolution re-encoded for export, well above what the small on-screen preview needs. */
    private static final int EXPORT_QR_RESOLUTION = 1000;

    private final KhqrFormView.Layout layout;
    private final KhqrFormFields fields;
    private final KhqrCardView card;
    private final KhqrService khqrService = new KhqrService();
    private final QrImageCodec qrImageCodec = new QrImageCodec();

    public KhqrFormController(KhqrFormView.Layout layout) {
        this.layout = layout;
        this.fields = layout.fields();
        this.card = layout.card();
    }

    public void wireActions() {
        layout.generateButton().setOnAction(e -> onGenerate());
        layout.clearButton().setOnAction(e -> onClear());
        layout.selectFileButton().setOnAction(e -> onSelectFile());
        layout.decodeButton().setOnAction(e -> onDecode());
        layout.copyQrButton().setOnAction(e -> copyToClipboard(fields.qrCodeInput().getText()));
        layout.copyJsonButton().setOnAction(e -> copyToClipboard(fields.jsonResultArea().getText()));
        layout.saveImageButton().setOnAction(e -> onSaveImage());
        wireDropZone(layout.dropZone());
    }

    private void onGenerate() {
        MerchantType merchantType = MerchantType.fromDisplay(fields.merchantTypeInput().getValue());
        List<String> errors = FormValidator.validate(fields, merchantType);
        if (!errors.isEmpty()) {
            setStatus(String.join("\n", errors), true);
            return;
        }
        try {
            Currency currency = Currency.fromDisplay(fields.transactionCurrencyInput().getValue());
            String categoryCode = fields.merchantCategoryCodeInput().getText();

            KhqrService.GenerationOutcome outcome = merchantType.isIndividual()
                    ? khqrService.generateIndividual(KhqrFormMapper.toIndividualInfo(fields, currency), categoryCode)
                    : khqrService.generateMerchant(KhqrFormMapper.toMerchantInfo(fields, currency), categoryCode);

            fields.jsonResultArea().setText(outcome.json());
            if (!outcome.success()) {
                throw new IllegalStateException(outcome.errorMessage());
            }

            fields.qrCodeInput().setText(outcome.qrCode());
            fields.timeStampLabel().setText(Timestamps.nowStamp());
            fields.qrImageView().setImage(qrImageCodec.encode(outcome.qrCode(), 400, 400));
            refreshCard();
            setStatus("Generated KHQR Image", false);
        } catch (RuntimeException ex) {
            showError("Invalid: " + ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onClear() {
        fields.merchantTypeInput().setValue(MerchantType.REMITTANCE.display());
        fields.bakongAccountIdInput().setText(FormDefaults.BAKONG_ACCOUNT_ID);
        fields.merchantIdInput().clear();
        fields.accountInformationInput().setText(FormDefaults.ACCOUNT_INFORMATION);
        fields.acquiringBankInput().setText(FormDefaults.ACQUIRING_BANK);
        fields.merchantCategoryCodeInput().setText(FormDefaults.MERCHANT_CATEGORY_CODE);
        fields.countryCodeInput().setText(FormDefaults.COUNTRY_CODE);
        fields.merchantNameInput().setText(FormDefaults.MERCHANT_NAME);
        fields.merchantCityInput().setText(FormDefaults.MERCHANT_CITY);
        fields.transactionCurrencyInput().setValue(Currency.USD.display());
        fields.transactionAmountInput().clear();
        fields.billNumberInput().clear();
        fields.storeLabelInput().clear();
        fields.terminalLabelInput().clear();
        fields.mobileNumberInput().clear();
        fields.timeStampLabel().setText("");
        fields.expireStampLabel().setText("");
        fields.qrCodeInput().clear();
        fields.qrImageView().setImage(null);
        fields.jsonResultArea().clear();
        fields.qrStringLabel().setText("");
        fields.qrStringLabel().getStyleClass().removeAll("status-error", "status-success");
        FormValidator.clearErrors(fields);
        refreshCard();
    }

    /** Reflects the current name/amount/currency fields onto the branded KHQR card preview. */
    private void refreshCard() {
        Currency currency = Currency.fromDisplay(fields.transactionCurrencyInput().getValue());
        card.setReceiverName(fields.merchantNameInput().getText());
        card.setAmount(fields.transactionAmountInput().getText(), currency);
    }

    private void onSelectFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            handleImageFile(selectedFile);
        }
    }

    private void onDecode() {
        String qrCodeText = fields.qrCodeInput().getText();
        if (StringUtils.isBlank(qrCodeText)) {
            return;
        }
        try {
            applyDecodeOutcome(khqrService.decodeAndVerify(qrCodeText));
            fields.qrImageView().setImage(qrImageCodec.encode(qrCodeText, 400, 400));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onSaveImage() {
        Image image = fields.qrImageView().getImage();
        if (image == null) {
            showError("No QR image to save");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("khqr.png");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        try {
            PngImageWriter.write(renderExportImage(), file);
            setStatus("Saved QR image to " + file.getName(), false);
        } catch (Exception ex) {
            showError("Failed to save image: " + ex.getMessage());
        }
    }

    /**
     * Renders the card at export quality onto a fixed {@code 750x1200} canvas: the QR is
     * re-encoded at {@value #EXPORT_QR_RESOLUTION}px (the live preview only needs 400px, which
     * would look soft blown up this far), the whole card is scaled up as large as it fits without
     * distorting its 20:29 ratio, and centered on a white canvas so every export is exactly the
     * same size regardless of the on-screen preview's pixel dimensions.
     */
    private WritableImage renderExportImage() throws Exception {
        String qrText = fields.qrCodeInput().getText();
        Image liveQrImage = fields.qrImageView().getImage();
        if (StringUtils.isBlank(qrText)) {
            return renderCardAt(EXPORT_WIDTH, EXPORT_HEIGHT);
        }

        fields.qrImageView().setImage(qrImageCodec.encode(qrText, EXPORT_QR_RESOLUTION, EXPORT_QR_RESOLUTION));
        try {
            return renderCardAt(EXPORT_WIDTH, EXPORT_HEIGHT);
        } finally {
            fields.qrImageView().setImage(liveQrImage);
        }
    }

    private WritableImage renderCardAt(double canvasWidth, double canvasHeight) {
        // boundsInLocal (not getWidth/getHeight) includes the card's drop shadow, which the
        // snapshot transform below scales along with everything else.
        Bounds bounds = card.getBoundsInLocal();
        double scale = Math.min(canvasWidth / bounds.getWidth(), canvasHeight / bounds.getHeight());
        SnapshotParameters cardParams = new SnapshotParameters();
        cardParams.setTransform(new Scale(scale, scale));
        cardParams.setFill(Color.WHITE);
        Image scaledCard = card.snapshot(cardParams, null);

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        gc.drawImage(scaledCard, (canvasWidth - scaledCard.getWidth()) / 2, (canvasHeight - scaledCard.getHeight()) / 2);

        SnapshotParameters canvasParams = new SnapshotParameters();
        canvasParams.setFill(Color.WHITE);
        return canvas.snapshot(canvasParams, new WritableImage((int) canvasWidth, (int) canvasHeight));
    }

    private void wireDropZone(Node dropZone) {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragEntered(event -> dropZone.getStyleClass().add("drop-zone-active"));
        dropZone.setOnDragExited(event -> dropZone.getStyleClass().remove("drop-zone-active"));
        dropZone.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean accepted = dragboard.hasFiles() && !dragboard.getFiles().isEmpty();
            if (accepted) {
                handleImageFile(dragboard.getFiles().get(0));
            }
            event.setDropCompleted(accepted);
            event.consume();
        });
    }

    /**
     * Reads the QR out of a dropped or chosen file. The file's own artwork never reaches the card —
     * the frame always shows a QR re-encoded from the decoded payload, so it stays on-brand
     * whatever the source image looked like.
     */
    private void handleImageFile(File file) {
        try {
            String decodedText = qrImageCodec.decode(new Image(file.toURI().toString()));
            fields.qrCodeInput().setText(decodedText);
            applyDecodeOutcome(khqrService.decodeAndVerify(decodedText));
            fields.qrImageView().setImage(qrImageCodec.encode(decodedText, 400, 400));
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void applyDecodeOutcome(KhqrService.DecodeOutcome outcome) {
        fields.jsonResultArea().setText(outcome.json());
        if (outcome.data() != null) {
            KhqrFormMapper.applyDecodedData(outcome.data(), fields);
            refreshCard();
        }
        if (!outcome.valid()) {
            throw new IllegalStateException("QR Code is not valid: " + outcome.errorMessage());
        }
        setStatus("QR Code is valid", false);
    }

    private void copyToClipboard(String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void showError(String message) {
        fields.qrImageView().setImage(null);
        setStatus(message, true);
    }

    private void setStatus(String message, boolean error) {
        fields.qrStringLabel().setText(message);
        fields.qrStringLabel().getStyleClass().removeAll("status-error", "status-success");
        fields.qrStringLabel().getStyleClass().add(error ? "status-error" : "status-success");
    }
}
