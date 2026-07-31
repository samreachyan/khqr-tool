package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.khqr.KhqrFormMapper;
import com.sakcode.decodekhqr.khqr.KhqrService;
import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import com.sakcode.decodekhqr.qr.PngImageWriter;
import com.sakcode.decodekhqr.qr.QrImageCodec;
import com.sakcode.decodekhqr.util.Timestamps;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/** Wires the form's buttons to {@link KhqrService}/{@link QrImageCodec} and reflects results back onto the UI. */
public final class KhqrFormController {

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
            WritableImage snapshot = card.snapshot(new SnapshotParameters(), null);
            PngImageWriter.write(snapshot, file);
            setStatus("Saved QR image to " + file.getName(), false);
        } catch (Exception ex) {
            showError("Failed to save image: " + ex.getMessage());
        }
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

    private void handleImageFile(File file) {
        try {
            Image qrImage = new Image(file.toURI().toString());
            fields.qrImageView().setImage(qrImage);
            String decodedText = qrImageCodec.decode(qrImage);
            fields.qrCodeInput().setText(decodedText);
            applyDecodeOutcome(khqrService.decodeAndVerify(decodedText));
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
