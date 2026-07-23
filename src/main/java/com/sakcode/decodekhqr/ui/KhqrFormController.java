package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.khqr.KhqrFormMapper;
import com.sakcode.decodekhqr.khqr.KhqrService;
import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import com.sakcode.decodekhqr.qr.QrImageCodec;
import com.sakcode.decodekhqr.util.Timestamps;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/** Wires the form's buttons to {@link KhqrService}/{@link QrImageCodec} and reflects results back onto the UI. */
public final class KhqrFormController {

    private final KhqrFormView.Layout layout;
    private final KhqrFormFields fields;
    private final KhqrService khqrService = new KhqrService();
    private final QrImageCodec qrImageCodec = new QrImageCodec();

    public KhqrFormController(KhqrFormView.Layout layout) {
        this.layout = layout;
        this.fields = layout.fields();
    }

    public void wireActions() {
        layout.generateButton().setOnAction(e -> onGenerate());
        layout.selectFileButton().setOnAction(e -> onSelectFile());
        layout.decodeButton().setOnAction(e -> onDecode());
    }

    private void onGenerate() {
        try {
            MerchantType merchantType = MerchantType.fromDisplay(fields.merchantTypeInput().getValue());
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
            fields.qrStringLabel().setText("Generated KHQR Image");
            fields.timeStampLabel().setText(Timestamps.nowStamp());
            fields.qrImageView().setImage(qrImageCodec.encode(outcome.qrCode(), 400, 400));
        } catch (RuntimeException ex) {
            showError("Invalid: " + ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onSelectFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile == null) {
            return;
        }
        try {
            Image qrImage = new Image(selectedFile.toURI().toString());
            fields.qrImageView().setImage(qrImage);
            String decodedText = qrImageCodec.decode(qrImage);
            fields.qrCodeInput().setText(decodedText);
            applyDecodeOutcome(khqrService.decodeAndVerify(decodedText));
        } catch (Exception ex) {
            showError(ex.getMessage());
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

    private void applyDecodeOutcome(KhqrService.DecodeOutcome outcome) {
        fields.jsonResultArea().setText(outcome.json());
        if (outcome.data() != null) {
            KhqrFormMapper.applyDecodedData(outcome.data(), fields);
        }
        if (!outcome.valid()) {
            throw new IllegalStateException("QR Code is not valid: " + outcome.errorMessage());
        }
        fields.qrStringLabel().setText("QR Code is valid");
    }

    private void showError(String message) {
        fields.qrImageView().setImage(null);
        fields.qrStringLabel().setText(message);
        fields.qrStringLabel().setStyle(UiStyles.ERROR_LABEL);
    }
}
