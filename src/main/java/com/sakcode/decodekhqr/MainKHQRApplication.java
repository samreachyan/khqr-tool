package com.sakcode.decodekhqr;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import kh.org.nbc.bakong_khqr.BakongKHQR;
import kh.org.nbc.bakong_khqr.model.*;
import kh.org.nbc.bakong_khqr.utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainKHQRApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("QR Code Generator and Decoder");

        // Left box: Input fields and buttons
        GridPane inputGrid = new GridPane();
        inputGrid.setPadding(new Insets(10, 10, 10, 10));
        inputGrid.setVgap(8);
        inputGrid.setHgap(10);

        Label payloadFormatIndicatorLabel = new Label("Payload Format Indicator:");
        GridPane.setConstraints(payloadFormatIndicatorLabel, 0, 0);
        TextField payloadFormatIndicatorInput = new TextField("01");
        GridPane.setConstraints(payloadFormatIndicatorInput, 1, 0);

        Label pointOfInitiationMethodLabel = new Label("Point of Initiation Method:");
        GridPane.setConstraints(pointOfInitiationMethodLabel, 0, 1);
        ComboBox<String> pointOfInitiationInput = new ComboBox<>();
        pointOfInitiationInput.getItems().addAll("11", "12");
        pointOfInitiationInput.setValue("11");
        GridPane.setConstraints(pointOfInitiationInput, 1, 1);

        Label merchantTypeLabel = new Label("Merchant Type:");
        GridPane.setConstraints(merchantTypeLabel, 0, 2);
        TextField merchantTypeInput = new TextField("29");
        GridPane.setConstraints(merchantTypeInput, 1, 2);

        Label bakongAccountIDLabel = new Label("Bakong Account ID:");
        GridPane.setConstraints(bakongAccountIDLabel, 0, 3);
        TextField bakongAccountIDInput = new TextField("ftcckhppxxx@ftcc");
        GridPane.setConstraints(bakongAccountIDInput, 1, 3);

        Label merchantIdLabel = new Label("Merchant ID:");
        GridPane.setConstraints(merchantIdLabel, 0, 4);
        TextField merchantIdInput = new TextField();
        GridPane.setConstraints(merchantIdInput, 1, 4);

        Label accountInformationLabel = new Label("Account Information:");
        GridPane.setConstraints(accountInformationLabel, 0, 5);
        TextField accountInformationInput = new TextField("123123");
        GridPane.setConstraints(accountInformationInput, 1, 5);

        Label acquiringBankLabel = new Label("Acquiring Bank:");
        GridPane.setConstraints(acquiringBankLabel, 0, 6);
        TextField acquiringBankInput = new TextField("Foreign Trade Bank of Cambodia");
        GridPane.setConstraints(acquiringBankInput, 1, 6);

        Label merchantCategoryCodeLabel = new Label("Merchant Category Code:");
        GridPane.setConstraints(merchantCategoryCodeLabel, 0, 7);
        TextField merchantCategoryCodeInput = new TextField("5999");
        GridPane.setConstraints(merchantCategoryCodeInput, 1, 7);

        Label countryCodeLabel = new Label("Country Code:");
        GridPane.setConstraints(countryCodeLabel, 0, 8);
        TextField countryCodeInput = new TextField("KH");
        GridPane.setConstraints(countryCodeInput, 1, 8);

        Label merchantNameLabel = new Label("Merchant Name:");
        GridPane.setConstraints(merchantNameLabel, 0, 9);
        TextField merchantNameInput = new TextField("Samreach");
        GridPane.setConstraints(merchantNameInput, 1, 9);

        Label merchantCityLabel = new Label("Merchant City:");
        GridPane.setConstraints(merchantCityLabel, 0, 10);
        TextField merchantCityInput = new TextField("PHNOM PENH");
        GridPane.setConstraints(merchantCityInput, 1, 10);

        Label transactionCurrencyLabel = new Label("Transaction Currency:");
        GridPane.setConstraints(transactionCurrencyLabel, 0, 11);
        ComboBox<String> transactionCurrencyInput = new ComboBox<>();
        transactionCurrencyInput.getItems().addAll("840", "116");
        transactionCurrencyInput.setValue("840");
        GridPane.setConstraints(transactionCurrencyInput, 1, 11);

        Label transactionAmountLabel = new Label("Transaction Amount:");
        GridPane.setConstraints(transactionAmountLabel, 0, 12);
        TextField transactionAmountInput = new TextField();
        GridPane.setConstraints(transactionAmountInput, 1, 12);

        Label billNumberLabel = new Label("Bill Number:");
        GridPane.setConstraints(billNumberLabel, 0, 13);
        TextField billNumberInput = new TextField();
        GridPane.setConstraints(billNumberInput, 1, 13);

        Label storeLabelLabel = new Label("Store Label:");
        GridPane.setConstraints(storeLabelLabel, 0, 14);
        TextField storeLabelInput = new TextField();
        GridPane.setConstraints(storeLabelInput, 1, 14);

        Label terminalLabelLabel = new Label("Terminal Label:");
        GridPane.setConstraints(terminalLabelLabel, 0, 15);
        TextField terminalLabelInput = new TextField();
        GridPane.setConstraints(terminalLabelInput, 1, 15);

        Label mobileNumberLabel = new Label("Mobile Number:");
        GridPane.setConstraints(mobileNumberLabel, 0, 16);
        TextField mobileNumberInput = new TextField();
        GridPane.setConstraints(mobileNumberInput, 1, 16);

        Label timeStamp = new Label("Time stamp: ");
        GridPane.setConstraints(timeStamp, 0, 17);
        Label timeStampInput = new Label();
        GridPane.setConstraints(timeStampInput, 1, 17);

        Button selectFileButton = new Button("Select File");
        GridPane.setConstraints(selectFileButton, 0, 18);
        TextArea qrCodeInput = new TextArea();
        GridPane.setConstraints(qrCodeInput, 1, 18);

//        Label merchantCityLabel = new Label("Merchant City:");
//        GridPane.setConstraints(merchantCityLabel, 0, 10);
//        TextField merchantCityInput = new TextField("PHNOM PENH");
//        GridPane.setConstraints(merchantCityInput, 1, 10);

        Button decodeQRButton = new Button("Decode QR");
        GridPane.setConstraints(decodeQRButton, 0, 19);

        Button generateQRButton = new Button("Generate QR");
        GridPane.setConstraints(generateQRButton, 1, 19);

        inputGrid.getChildren().addAll(payloadFormatIndicatorLabel, payloadFormatIndicatorInput,
                pointOfInitiationMethodLabel, pointOfInitiationInput,
                merchantTypeLabel, merchantTypeInput,
                bakongAccountIDLabel, bakongAccountIDInput,
                merchantIdLabel, merchantIdInput,
                accountInformationLabel, accountInformationInput,
                acquiringBankLabel, acquiringBankInput,
                merchantCategoryCodeLabel, merchantCategoryCodeInput,
                countryCodeLabel, countryCodeInput,
                merchantNameLabel, merchantNameInput,
                merchantCityLabel, merchantCityInput,
                transactionCurrencyLabel, transactionCurrencyInput,
                transactionAmountLabel, transactionAmountInput,
                billNumberLabel, billNumberInput, storeLabelLabel,
                storeLabelInput, terminalLabelLabel,
                terminalLabelInput, mobileNumberLabel,
                mobileNumberInput, generateQRButton,
                selectFileButton, qrCodeInput,
                decodeQRButton, timeStamp, timeStampInput);

        // Right box: Display QR code and string
        VBox qrDisplayBox = new VBox(10);
        qrDisplayBox.setPadding(new Insets(10, 10, 10, 10));
        ImageView qrImageView = new ImageView();
        qrImageView.setFitWidth(400);
        qrImageView.setFitHeight(400);
        Label qrStringLabel = new Label();

        qrDisplayBox.getChildren().addAll(qrImageView, qrStringLabel);

        // Main layout
        HBox mainLayout = new HBox(20);
        mainLayout.getChildren().addAll(inputGrid, qrDisplayBox);

        Scene scene = new Scene(mainLayout, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Add functionality to the buttons (this is a placeholder, actual QR generation/decoding logic is needed)
        generateQRButton.setOnAction(e -> {
            // Generate QR code logic here
            // For demonstration, we'll just update the label
            IndividualInfo individualInfo = new IndividualInfo();
            individualInfo.setAccountInformation(accountInformationInput.getText());
            individualInfo.setAcquiringBank(acquiringBankInput.getText());
            individualInfo.setBakongAccountId(bakongAccountIDInput.getText());
            individualInfo.setCurrency(transactionCurrencyInput.getValue().equalsIgnoreCase("116") ? KHQRCurrency.KHR : KHQRCurrency.USD);
            individualInfo.setMerchantName(merchantNameInput.getText());
            individualInfo.setMerchantCity(merchantCityInput.getText());

            if (StringUtils.isNotBlank(mobileNumberInput.getText())) individualInfo.setMobileNumber(mobileNumberInput.getText());
            if (StringUtils.isNotBlank(transactionAmountInput.getText())) individualInfo.setAmount(Double.valueOf(transactionAmountInput.getText()));
            if (StringUtils.isNotBlank(billNumberInput.getText())) individualInfo.setBillNumber(billNumberInput.getText());

            try {
                KHQRResponse<KHQRData> khqrResponse = BakongKHQR.generateIndividual(individualInfo);
                if (khqrResponse.getKHQRStatus().getCode() == 0) {
                    String qrCode = khqrResponse.getData().getQr();

                    System.out.println("Generated KHQR: " + qrCode);
                    qrStringLabel.setText("Generated KHQR Image");
                    qrCodeInput.setText(qrCode);

                    // Update the ImageView with the generated QR image
                    Image qrImage = generateQRCodeImage(qrCode, 400, 400);
                    qrImageView.setImage(qrImage);
                } else {
                    qrImageView.setImage(null);
                    qrCodeInput.setText(null);
                    qrStringLabel.setText(khqrResponse.getKHQRStatus().getMessage());
                }
            } catch (WriterException | IOException ex) {
                ex.printStackTrace();
                qrStringLabel.setText(ex.getMessage());
            }
            // qrImageView.setImage(generatedQRImage);
        });

        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                // Decode QR code from the selected file
                // For demonstration, we'll just update the label
                qrStringLabel.setText("Decoded QR String from file: " + selectedFile.getName());
                // Update the ImageView with the selected QR image
                Image qrImage = new Image(selectedFile.toURI().toString());
                qrImageView.setImage(qrImage);

                try {
                    BufferedImage bufferedImage = ImageIO.read(selectedFile);

                    // Convert the image to a binary bitmap source
                    LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    // Decode the QR code
                    Result result = new MultiFormatReader().decode(bitmap);

                    // Print the result
                    System.out.println("QR Code text: " + result.getText());
                    qrCodeInput.setText(result.getText());

                    // Decode QR and Set value to extract field above:
                    if (StringUtils.isNotBlank(result.getText())) {
                        String qrCodeStr = result.getText();
                        KHQRResponse<KHQRDecodeData> decode = BakongKHQR.decode(qrCodeStr);
                        KHQRResponse<CRCValidation> valid = BakongKHQR.verify(qrCodeStr);

                        System.out.println(decode.getData());

                        if (valid.getKHQRStatus().getCode() == 0) {
                            System.out.println("QR Code is valid");
                            qrStringLabel.setText("QR Code is valid");

                            // Set to text field
                            payloadFormatIndicatorInput.setText(decode.getData().getPayloadFormatIndicator());
                            pointOfInitiationInput.setValue(decode.getData().getPointOfInitiationMethod());
                            merchantTypeInput.setText(decode.getData().getMerchantType());
                            bakongAccountIDInput.setText(decode.getData().getBakongAccountID());
                            merchantIdInput.setText(decode.getData().getMerchantId());
                            accountInformationInput.setText(decode.getData().getAccountInformation());
                            acquiringBankInput.setText(decode.getData().getAcquiringBank());
                            merchantCategoryCodeInput.setText(decode.getData().getMerchantCategoryCode());
                            merchantCityInput.setText(decode.getData().getMerchantCity());
                            merchantNameInput.setText(decode.getData().getMerchantName());
                            countryCodeInput.setText(decode.getData().getCountryCode());
                            transactionCurrencyInput.setValue(decode.getData().getTransactionCurrency());
                            transactionAmountInput.setText(decode.getData().getTransactionAmount());
                            billNumberInput.setText(decode.getData().getBillNumber());
                            storeLabelInput.setText(decode.getData().getStoreLabel());
                            terminalLabelInput.setText(decode.getData().getTerminalLabel());
                            mobileNumberInput.setText(decode.getData().getMobileNumber());
                            timeStampInput.setText(decode.getData().getTimestamp() + " - " + getUTC7DateTime(Long.parseLong(decode.getData().getTimestamp())));

                        } else {
                            System.out.println("QR Code is not valid");
                            qrStringLabel.setText("QR Code is not valid");
                        }
                    }

                } catch (Exception exception) {
                    System.err.println("There is no QR code in the image: " + exception);
                    qrStringLabel.setText(exception.getMessage());
                }

            }
        });

        decodeQRButton.setOnAction(e -> {
            // Decode QR code logic here
            // For demonstration, we'll just update the label
            qrStringLabel.setText("Decoded QR String");

            if (StringUtils.isNotBlank(qrCodeInput.getText())) {

                String qrCodeStr = qrCodeInput.getText();
                KHQRResponse<KHQRDecodeData> decode = BakongKHQR.decode(qrCodeStr);
                KHQRResponse<CRCValidation> valid = BakongKHQR.verify(qrCodeStr);

                System.out.println(decode.getData());

                if (valid.getKHQRStatus().getCode() == 0) {
                    System.out.println("QR Code is valid");
                    qrStringLabel.setText("QR Code is valid");

                    // Set to text field
                    payloadFormatIndicatorInput.setText(decode.getData().getPayloadFormatIndicator());
                    pointOfInitiationInput.setValue(decode.getData().getPointOfInitiationMethod());
                    merchantTypeInput.setText(decode.getData().getMerchantType());
                    bakongAccountIDInput.setText(decode.getData().getBakongAccountID());
                    merchantIdInput.setText(decode.getData().getMerchantId());
                    accountInformationInput.setText(decode.getData().getAccountInformation());
                    acquiringBankInput.setText(decode.getData().getAcquiringBank());
                    merchantCategoryCodeInput.setText(decode.getData().getMerchantCategoryCode());
                    merchantCityInput.setText(decode.getData().getMerchantCity());
                    merchantNameInput.setText(decode.getData().getMerchantName());
                    countryCodeInput.setText(decode.getData().getCountryCode());
                    transactionCurrencyInput.setValue(decode.getData().getTransactionCurrency());
                    transactionAmountInput.setText(decode.getData().getTransactionAmount());
                    billNumberInput.setText(decode.getData().getBillNumber());
                    storeLabelInput.setText(decode.getData().getStoreLabel());
                    terminalLabelInput.setText(decode.getData().getTerminalLabel());
                    mobileNumberInput.setText(decode.getData().getMobileNumber());
                    timeStampInput.setText(decode.getData().getTimestamp() + " - " + getUTC7DateTime(Long.parseLong(decode.getData().getTimestamp())));

                    // qrImageView.setImage(generatedQRImage);
                    try {
                        Image qrImage = generateQRCodeImage(qrCodeStr, 400, 400);
                        qrImageView.setImage(qrImage);
                    } catch (WriterException | IOException ex) {
                        ex.printStackTrace();
                    }

                } else {
                    System.out.println("QR Code is not valid");
                    qrStringLabel.setText("QR Code is not valid");
                }

            }
        });
    }

    private String getUTC7DateTime(long currentTime) {

        // Get the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();

        // Convert milliseconds to LocalDateTime in UTC+0
        Instant instant = Instant.ofEpochMilli(currentTimeMillis);
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

        // Convert UTC time to UTC+7
        LocalDateTime utcPlus7DateTime = utcDateTime.plusHours(7);

        // Format the date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
        String formattedDateTime = utcPlus7DateTime.format(formatter);

        // Output the formatted date and time
        System.out.println("UTC+7 Time: " + formattedDateTime);
        return formattedDateTime;
    }

    private Image generateQRCodeImage(String qrText, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return convertToFxImage(bufferedImage);
    }

    private static Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }

        return new ImageView(wr).getImage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}