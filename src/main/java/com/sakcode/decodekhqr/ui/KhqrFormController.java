package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.history.HistoryEntry;
import com.sakcode.decodekhqr.history.HistoryService;
import com.sakcode.decodekhqr.khqr.KhqrFormMapper;
import com.sakcode.decodekhqr.khqr.KhqrService;
import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import com.sakcode.decodekhqr.qr.PngImageWriter;
import com.sakcode.decodekhqr.qr.QrImageCodec;
import com.sakcode.decodekhqr.util.Timestamps;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

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
    private final HistoryService historyService;
    private final DateTimeFormatter historyTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Timeline countdownTimeline;
    private Long expirationMillis;

    public KhqrFormController(KhqrFormView.Layout layout, HistoryService historyService) {
        this.layout = layout;
        this.fields = layout.fields();
        this.card = layout.card();
        this.historyService = historyService;
    }

    public void wireActions() {
        layout.generateButton().setOnAction(e -> onGenerate());
        layout.clearButton().setOnAction(e -> onClear());
        layout.selectFileButton().setOnAction(e -> onSelectFile());
        layout.scanCameraButton().setOnAction(e -> onScanCamera());
        layout.decodeButton().setOnAction(e -> onDecode());
        layout.copyQrButton().setOnAction(e -> copyToClipboard(fields.qrCodeInput().getText()));
        layout.copyJsonButton().setOnAction(e -> copyToClipboard(fields.jsonResultArea().getText()));
        layout.copyImageButton().setOnAction(e -> onCopyImage());
        layout.saveImageButton().setOnAction(e -> onSaveImage());
        layout.printSheetButton().setOnAction(e -> onPrintSheet());
        wireDropZone(layout.dropZone());
        wireLiveValidation();
        wireAmountFormatter();
        wireHistoryList();
        refreshHistoryList();
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

            saveHistoryEntry("GENERATED", outcome.qrCode(), outcome.json(), currency);
            startCountdownIfDynamic();
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
        stopCountdown();
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
            KhqrService.DecodeOutcome outcome = khqrService.decodeAndVerify(qrCodeText);
            applyDecodeOutcome(outcome);
            fields.qrImageView().setImage(qrImageCodec.encode(qrCodeText, 400, 400));

            Currency currency = Currency.fromCode(outcome.data() != null ? outcome.data().getTransactionCurrency() : "840");
            saveHistoryEntry("DECODED", qrCodeText, outcome.json(), currency);
            startCountdownFromDecoded(outcome);
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

    // ── Webcam Scanner ───────────────────────────────────────────────────────────

    private void onScanCamera() {
        Optional<String> result = new WebcamScanDialog().showAndWait(layout.root().getScene().getWindow());
        result.ifPresent(qrText -> {
            fields.qrCodeInput().setText(qrText);
            onDecode();
        });
    }

    // ── Copy Image to Clipboard ──────────────────────────────────────────────────

    private void onCopyImage() {
        try {
            WritableImage image = renderExportImage();
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            Clipboard.getSystemClipboard().setContent(content);
            setStatus("Image copied to clipboard", false);
        } catch (Exception ex) {
            showError("Failed to copy image: " + ex.getMessage());
        }
    }

    // ── Print Sheet ──────────────────────────────────────────────────────────────

    private void onPrintSheet() {
        try {
            WritableImage cardImage = renderExportImage();
            new PrintSheetDialog(cardImage).show(layout.root().getScene().getWindow());
        } catch (Exception ex) {
            showError("Failed to prepare print sheet: " + ex.getMessage());
        }
    }

    // ── Live Validation ──────────────────────────────────────────────────────────

    private void wireLiveValidation() {
        List<TextField> requiredFields = List.of(
                fields.bakongAccountIdInput(), fields.acquiringBankInput(),
                fields.merchantCategoryCodeInput(), fields.merchantNameInput(),
                fields.merchantCityInput()
        );
        for (TextField field : requiredFields) {
            field.textProperty().addListener((obs, old, val) -> {
                if (StringUtils.isBlank(val)) {
                    if (!field.getStyleClass().contains(FormValidator.ERROR_CLASS)) {
                        field.getStyleClass().add(FormValidator.ERROR_CLASS);
                    }
                } else {
                    field.getStyleClass().remove(FormValidator.ERROR_CLASS);
                }
            });
        }
    }

    // ── Amount Formatter ─────────────────────────────────────────────────────────

    private void wireAmountFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[0-9.,]*")) {
                return change;
            }
            return null;
        };
        fields.transactionAmountInput().setTextFormatter(new TextFormatter<>(filter));

        fields.transactionAmountInput().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                formatAmountOnFocusLost();
            } else {
                String raw = fields.transactionAmountInput().getText().replace(",", "");
                fields.transactionAmountInput().setText(raw);
            }
        });
    }

    private void formatAmountOnFocusLost() {
        String text = fields.transactionAmountInput().getText().replace(",", "").trim();
        if (StringUtils.isBlank(text)) {
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(text);
            Currency currency = Currency.fromDisplay(fields.transactionCurrencyInput().getValue());
            int decimals = amount.scale() <= 0 ? 0 : Math.max(amount.scale(), currency.minorUnits());
            DecimalFormat format = new DecimalFormat("#,##0");
            format.setMinimumFractionDigits(decimals);
            format.setMaximumFractionDigits(decimals);
            fields.transactionAmountInput().setText(format.format(amount));
        } catch (NumberFormatException ignored) {
        }
    }

    // ── History ──────────────────────────────────────────────────────────────────

    private void wireHistoryList() {
        fields.historyListView().setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String time = Instant.ofEpochMilli(item.timestamp()).atZone(ZoneId.systemDefault()).format(historyTimeFormat);
                    String prefix = "[" + item.type() + "]";
                    String name = StringUtils.defaultString(item.merchantName(), "Unknown");
                    String amount = StringUtils.defaultString(item.amount(), "0");
                    String currency = StringUtils.defaultString(item.currency(), "USD");
                    setText(String.format("%s %s — %s %s — %s", prefix, name, amount, currency, time));
                }
            }
        });

        fields.historyListView().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                restoreHistoryEntry(fields.historyListView().getSelectionModel().getSelectedItem());
            }
        });

        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            HistoryEntry selected = fields.historyListView().getSelectionModel().getSelectedItem();
            if (selected != null) {
                historyService.delete(selected.id());
                refreshHistoryList();
            }
        });
        menu.getItems().add(deleteItem);
        fields.historyListView().setContextMenu(menu);
    }

    private void refreshHistoryList() {
        fields.historyListView().getItems().setAll(historyService.list());
    }

    private void saveHistoryEntry(String type, String qrString, String json, Currency currency) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("merchantType", fields.merchantTypeInput().getValue());
        snapshot.put("bakongAccountId", fields.bakongAccountIdInput().getText());
        snapshot.put("merchantId", fields.merchantIdInput().getText());
        snapshot.put("accountInformation", fields.accountInformationInput().getText());
        snapshot.put("acquiringBank", fields.acquiringBankInput().getText());
        snapshot.put("merchantCategoryCode", fields.merchantCategoryCodeInput().getText());
        snapshot.put("countryCode", fields.countryCodeInput().getText());
        snapshot.put("merchantName", fields.merchantNameInput().getText());
        snapshot.put("merchantCity", fields.merchantCityInput().getText());
        snapshot.put("transactionCurrency", fields.transactionCurrencyInput().getValue());
        snapshot.put("transactionAmount", fields.transactionAmountInput().getText());
        snapshot.put("billNumber", fields.billNumberInput().getText());
        snapshot.put("storeLabel", fields.storeLabelInput().getText());
        snapshot.put("terminalLabel", fields.terminalLabelInput().getText());
        snapshot.put("mobileNumber", fields.mobileNumberInput().getText());
        snapshot.put("timeStamp", fields.timeStampLabel().getText());
        snapshot.put("expireStamp", fields.expireStampLabel().getText());

        HistoryEntry entry = new HistoryEntry(
                null,
                System.currentTimeMillis(),
                type,
                qrString,
                json,
                fields.merchantNameInput().getText(),
                fields.transactionAmountInput().getText(),
                currency.display(),
                snapshot
        );
        historyService.save(entry);
        refreshHistoryList();
    }

    private void restoreHistoryEntry(HistoryEntry entry) {
        if (entry == null || entry.formSnapshot() == null) {
            return;
        }
        Map<String, String> s = entry.formSnapshot();
        fields.merchantTypeInput().setValue(s.getOrDefault("merchantType", MerchantType.REMITTANCE.display()));
        fields.bakongAccountIdInput().setText(s.getOrDefault("bakongAccountId", ""));
        fields.merchantIdInput().setText(s.getOrDefault("merchantId", ""));
        fields.accountInformationInput().setText(s.getOrDefault("accountInformation", ""));
        fields.acquiringBankInput().setText(s.getOrDefault("acquiringBank", ""));
        fields.merchantCategoryCodeInput().setText(s.getOrDefault("merchantCategoryCode", ""));
        fields.countryCodeInput().setText(s.getOrDefault("countryCode", ""));
        fields.merchantNameInput().setText(s.getOrDefault("merchantName", ""));
        fields.merchantCityInput().setText(s.getOrDefault("merchantCity", ""));
        fields.transactionCurrencyInput().setValue(s.getOrDefault("transactionCurrency", Currency.USD.display()));
        fields.transactionAmountInput().setText(s.getOrDefault("transactionAmount", ""));
        fields.billNumberInput().setText(s.getOrDefault("billNumber", ""));
        fields.storeLabelInput().setText(s.getOrDefault("storeLabel", ""));
        fields.terminalLabelInput().setText(s.getOrDefault("terminalLabel", ""));
        fields.mobileNumberInput().setText(s.getOrDefault("mobileNumber", ""));
        fields.timeStampLabel().setText(s.getOrDefault("timeStamp", ""));
        fields.expireStampLabel().setText(s.getOrDefault("expireStamp", ""));
        fields.qrCodeInput().setText(entry.qrString());
        fields.jsonResultArea().setText(entry.json());
        try {
            fields.qrImageView().setImage(qrImageCodec.encode(entry.qrString(), 400, 400));
        } catch (Exception ignored) {
        }
        refreshCard();
        FormValidator.clearErrors(fields);
        setStatus("Restored from history", false);
    }

    // ── Expiry Countdown ─────────────────────────────────────────────────────────

    private void startCountdownIfDynamic() {
        String amountText = fields.transactionAmountInput().getText();
        if (StringUtils.isBlank(amountText)) {
            stopCountdown();
            return;
        }
        try {
            new BigDecimal(amountText.replace(",", ""));
            expirationMillis = Timestamps.tenMinutesFromNowMillis();
            startCountdownTimeline();
        } catch (NumberFormatException e) {
            stopCountdown();
        }
    }

    private void startCountdownFromDecoded(KhqrService.DecodeOutcome outcome) {
        if (outcome.data() == null) {
            stopCountdown();
            return;
        }
        String expText = outcome.data().getExpirationTimestamp();
        if (StringUtils.isBlank(expText)) {
            stopCountdown();
            return;
        }
        try {
            expirationMillis = Long.parseLong(expText);
            if (expirationMillis > System.currentTimeMillis()) {
                startCountdownTimeline();
            } else {
                stopCountdown();
            }
        } catch (NumberFormatException e) {
            stopCountdown();
        }
    }

    private void startCountdownTimeline() {
        stopCountdown();
        fields.expiryCountdownLabel().setVisible(true);
        fields.expiryCountdownLabel().setManaged(true);
        fields.expiryCountdownLabel().getStyleClass().removeAll("status-error");

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdown();
    }

    private void updateCountdown() {
        if (expirationMillis == null) {
            stopCountdown();
            return;
        }
        long remaining = expirationMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            fields.expiryCountdownLabel().setText("Expired — regenerate");
            fields.expiryCountdownLabel().getStyleClass().add("status-error");
            stopCountdown();
            return;
        }
        long minutes = remaining / 60_000;
        long seconds = (remaining % 60_000) / 1000;
        fields.expiryCountdownLabel().setText(String.format("Expires in %02d:%02d", minutes, seconds));
        if (remaining <= 120_000) {
            fields.expiryCountdownLabel().getStyleClass().add("status-error");
        } else {
            fields.expiryCountdownLabel().getStyleClass().removeAll("status-error");
        }
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        expirationMillis = null;
        fields.expiryCountdownLabel().setVisible(false);
        fields.expiryCountdownLabel().setManaged(false);
        fields.expiryCountdownLabel().getStyleClass().removeAll("status-error");
    }
}
