package com.sakcode.decodekhqr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.*;
import kh.gov.nbc.bakong_khqr.utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainKHQRApplication extends Application {
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Currency mapping: Display value -> Backend code
    private static final Map<String, String> CURRENCY_MAP = new HashMap<>();
    static {
        CURRENCY_MAP.put("USD", "840");
        CURRENCY_MAP.put("KHR", "116");
    }

    // Reverse mapping: Backend code -> Display value
    private static final Map<String, String> REVERSE_CURRENCY_MAP = new HashMap<>();
    static {
        REVERSE_CURRENCY_MAP.put("840", "USD");
        REVERSE_CURRENCY_MAP.put("116", "KHR");
    }

    // Currency mapping: Display value -> Backend code
    private static final Map<String, String> MERCHANT_TYPE_MAP = new HashMap<>();
    static {
        MERCHANT_TYPE_MAP.put("Remittance", "29");
        MERCHANT_TYPE_MAP.put("Merchant", "30");
    }

    // Reverse mapping: Backend code -> Display value
    private static final Map<String, String> REVERSE_MERCHANT_TYPE_MAP = new HashMap<>();
    static {
        REVERSE_MERCHANT_TYPE_MAP.put("29", "Remittance");
        REVERSE_MERCHANT_TYPE_MAP.put("30", "Merchant");
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("QR Code Generator and Decoder - FTB Samreach");

        // Root layout
        HBox root = new HBox(20);
        root.setPadding(new Insets(10));
        HBox.setHgrow(root, Priority.ALWAYS);

        // Left side: Input panel
        VBox inputPanel = new VBox(10);
        inputPanel.setPrefWidth(500);
        inputPanel.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Right side: QR display
        VBox qrDisplayBox = new VBox(10);
        qrDisplayBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(qrDisplayBox, Priority.ALWAYS);

        ImageView qrImageView = new ImageView();
        qrImageView.fitWidthProperty().bind(qrDisplayBox.widthProperty().multiply(0.9));
        qrImageView.fitHeightProperty().bind(qrDisplayBox.heightProperty().multiply(0.7));
        qrImageView.setPreserveRatio(true);

        Label qrStringLabel = new Label();
        qrStringLabel.setWrapText(true);
        qrStringLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(qrStringLabel, Priority.ALWAYS);

        // Add TextArea for JSON result
        TextArea jsonResultArea = new TextArea();
        jsonResultArea.setPrefRowCount(10);
        jsonResultArea.setWrapText(true);
        jsonResultArea.setEditable(false);
        jsonResultArea.setMaxWidth(Double.MAX_VALUE);
//        jsonResultArea.setMaxHeight(200);
        VBox.setVgrow(jsonResultArea, Priority.ALWAYS);
        qrDisplayBox.getChildren().addAll(qrImageView, qrStringLabel, jsonResultArea);

        // Create input grid and set up button actions
        GridPane inputGrid = createInputGrid(qrImageView, qrStringLabel, jsonResultArea);
        scrollPane.setContent(inputGrid);
        inputPanel.getChildren().add(scrollPane);

        // Add to root
        root.getChildren().addAll(inputPanel, qrDisplayBox);

        // Scene setup
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Bind input panel width dynamically
        inputPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
    }

    private GridPane createInputGrid(ImageView qrImageView, Label qrStringLabel, TextArea jsonResultArea) {
        GridPane inputGrid = new GridPane();
        inputGrid.setPadding(new Insets(10));
        inputGrid.setVgap(8);
        inputGrid.setHgap(10);

        // Define column constraints for responsive sizing
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHgrow(Priority.SOMETIMES);
        labelColumn.setPercentWidth(40); // 40% for labels
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        inputColumn.setPercentWidth(60); // 60% for inputs
        inputGrid.getColumnConstraints().addAll(labelColumn, inputColumn);

        // Input fields
        TextField payloadFormatIndicatorInput = new TextField("01");
        payloadFormatIndicatorInput.setDisable(true);
        ComboBox<String> pointOfInitiationInput = new ComboBox<>();
        pointOfInitiationInput.getItems().addAll("11", "12");
        pointOfInitiationInput.setValue("11");
        pointOfInitiationInput.setDisable(true);
        pointOfInitiationInput.setMaxWidth(Double.MAX_VALUE);
//        TextField merchantTypeInput = new TextField("29");
        ComboBox<String> merchantTypeInput = new ComboBox<>();
        merchantTypeInput.getItems().addAll("Remittance", "Merchant"); // Updated to 29, 39
        merchantTypeInput.setValue("Remittance"); // Default to USD
        merchantTypeInput.setMaxWidth(Double.MAX_VALUE);
        TextField bakongAccountIDInput = new TextField("ftcckhppxxx@ftcc");
        TextField merchantIdInput = new TextField();
        TextField accountInformationInput = new TextField("123123");
        TextField acquiringBankInput = new TextField("Foreign Trade Bank of Cambodia");
        TextField merchantCategoryCodeInput = new TextField("5999");
        TextField countryCodeInput = new TextField("KH");
        TextField merchantNameInput = new TextField("Samreach");
        TextField merchantCityInput = new TextField("PHNOM PENH");
        ComboBox<String> transactionCurrencyInput = new ComboBox<>();
        transactionCurrencyInput.getItems().addAll("USD", "KHR"); // Updated to USD, KHR
        transactionCurrencyInput.setValue("USD"); // Default to USD
        transactionCurrencyInput.setMaxWidth(Double.MAX_VALUE);
        TextField transactionAmountInput = new TextField();
        TextField billNumberInput = new TextField();
        TextField storeLabelInput = new TextField();
        TextField terminalLabelInput = new TextField();
        TextField mobileNumberInput = new TextField();
        Label timeStampInput = new Label();
        timeStampInput.setWrapText(true);
        Label expireStampInput = new Label();
        expireStampInput.setWrapText(true);

        TextArea qrCodeInput = new TextArea();
        qrCodeInput.setPrefRowCount(5);
        qrCodeInput.setWrapText(true);

        Button selectFileButton = new Button("Select File");
        Button decodeQRButton = new Button("Decode QR");
        Button generateQRButton = new Button("Generate QR");

        // Add to grid with responsive labels
        int row = 0;
        addGridRow(inputGrid, row++, "Payload Format Indicator:", payloadFormatIndicatorInput);
        addGridRow(inputGrid, row++, "Point of Initiation Method:", pointOfInitiationInput);
        addGridRow(inputGrid, row++, "QR Type:", merchantTypeInput);
        addGridRow(inputGrid, row++, "Bakong Account ID:", bakongAccountIDInput);
        addGridRow(inputGrid, row++, "Merchant ID:", merchantIdInput);
        addGridRow(inputGrid, row++, "Account Information:", accountInformationInput);
        addGridRow(inputGrid, row++, "Acquiring Bank:", acquiringBankInput);
        addGridRow(inputGrid, row++, "Merchant Category Code:", merchantCategoryCodeInput);
        addGridRow(inputGrid, row++, "Country Code:", countryCodeInput);
        addGridRow(inputGrid, row++, "Merchant Name:", merchantNameInput);
        addGridRow(inputGrid, row++, "Merchant City:", merchantCityInput);
        addGridRow(inputGrid, row++, "Transaction Currency:", transactionCurrencyInput);
        addGridRow(inputGrid, row++, "Transaction Amount:", transactionAmountInput);
        addGridRow(inputGrid, row++, "Bill Number:", billNumberInput);
        addGridRow(inputGrid, row++, "Store Label:", storeLabelInput);
        addGridRow(inputGrid, row++, "Terminal Label:", terminalLabelInput);
        addGridRow(inputGrid, row++, "Mobile Number:", mobileNumberInput);
        addGridRow(inputGrid, row++, "Created at:", timeStampInput);
        addGridRow(inputGrid, row++, "Expired at:", expireStampInput);
        addGridRow(inputGrid, row++, "QR Code Input:", qrCodeInput);

        // Buttons
        HBox buttonBox = new HBox(10, selectFileButton, decodeQRButton, generateQRButton);
        buttonBox.setAlignment(Pos.CENTER);
        GridPane.setConstraints(buttonBox, 0, row++, 2, 1);
        inputGrid.getChildren().add(buttonBox);

        // Set up button actions
        setupButtonActions(selectFileButton, decodeQRButton, generateQRButton, qrImageView, qrStringLabel, qrCodeInput,
                payloadFormatIndicatorInput, pointOfInitiationInput, merchantTypeInput, bakongAccountIDInput,
                merchantIdInput, accountInformationInput, acquiringBankInput, merchantCategoryCodeInput,
                countryCodeInput, merchantNameInput, merchantCityInput, transactionCurrencyInput,
                transactionAmountInput, billNumberInput, storeLabelInput, terminalLabelInput,
                mobileNumberInput, timeStampInput, expireStampInput, jsonResultArea);

        return inputGrid;
    }

    private void addGridRow(GridPane grid, int row, String labelText, Control control) {
        Label label = new Label(labelText);
        label.setWrapText(true); // Allow label text to wrap
        label.setMaxWidth(Double.MAX_VALUE); // Ensure label uses available space
        GridPane.setConstraints(label, 0, row);
        GridPane.setConstraints(control, 1, row);
        grid.getChildren().addAll(label, control);
        GridPane.setHgrow(control, Priority.ALWAYS); // Control expands to fill space
        if (control instanceof TextField) {
            ((TextField) control).setPrefColumnCount(15); // Reasonable default width
        }
    }

    private void setupButtonActions(Button selectFileButton, Button decodeQRButton, Button generateQRButton,
                                    ImageView qrImageView, Label qrStringLabel, TextArea qrCodeInput,
                                    TextField payloadFormatIndicatorInput, ComboBox<String> pointOfInitiationInput,
                                    ComboBox<String> merchantTypeInput, TextField bakongAccountIDInput, TextField merchantIdInput,
                                    TextField accountInformationInput, TextField acquiringBankInput,
                                    TextField merchantCategoryCodeInput, TextField countryCodeInput,
                                    TextField merchantNameInput, TextField merchantCityInput,
                                    ComboBox<String> transactionCurrencyInput, TextField transactionAmountInput,
                                    TextField billNumberInput, TextField storeLabelInput, TextField terminalLabelInput,
                                    TextField mobileNumberInput, Label timeStampInput, Label expireStampInput, TextArea jsonResultArea) {
        generateQRButton.setOnAction(e -> {
            try {
                String qrCode = generateQRCode(merchantTypeInput, bakongAccountIDInput, merchantIdInput,
                        accountInformationInput, acquiringBankInput, merchantNameInput, merchantCityInput, merchantCategoryCodeInput,
                        transactionCurrencyInput, transactionAmountInput, billNumberInput, storeLabelInput,
                        terminalLabelInput, mobileNumberInput, jsonResultArea);
                qrCodeInput.setText(qrCode);
                qrStringLabel.setText("Generated KHQR Image");

                ZoneId localZone = ZoneId.systemDefault();
                LocalDateTime now = LocalDateTime.now(localZone);
                timeStampInput.setText(now.atZone(localZone).toInstant().toEpochMilli() + " - " + getUTC7DateTime(now.atZone(localZone).toInstant().toEpochMilli()));

                qrImageView.setImage(generateQRCodeImage(qrCode, 400, 400));
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                qrImageView.setImage(null);
                qrStringLabel.setText("Invalid: " + ex.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                qrImageView.setImage(null);
                qrStringLabel.setText("Error generating QR.");
            }
        });

        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                try {
                    Image qrImage = new Image(selectedFile.toURI().toString());
                    qrImageView.setImage(qrImage);
                    String decodedText = decodeQRFromFile(selectedFile);
                    qrCodeInput.setText(decodedText);
                    updateFieldsFromDecodedQR(decodedText, qrStringLabel, payloadFormatIndicatorInput,
                            pointOfInitiationInput, merchantTypeInput, bakongAccountIDInput, merchantIdInput,
                            accountInformationInput, acquiringBankInput, merchantCategoryCodeInput, countryCodeInput,
                            merchantNameInput, merchantCityInput, transactionCurrencyInput, transactionAmountInput,
                            billNumberInput, storeLabelInput, terminalLabelInput, mobileNumberInput, timeStampInput, expireStampInput, jsonResultArea);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    jsonResultArea.clear();
                    qrImageView.setImage(null);
                    qrStringLabel.setText("Error decoding image QR.");
                }
            }
        });

        decodeQRButton.setOnAction(e -> {
            String qrCodeStr = qrCodeInput.getText();
            if (StringUtils.isNotBlank(qrCodeStr)) {
                try {
                    updateFieldsFromDecodedQR(qrCodeStr, qrStringLabel, payloadFormatIndicatorInput,
                            pointOfInitiationInput, merchantTypeInput, bakongAccountIDInput, merchantIdInput,
                            accountInformationInput, acquiringBankInput, merchantCategoryCodeInput, countryCodeInput,
                            merchantNameInput, merchantCityInput, transactionCurrencyInput, transactionAmountInput,
                            billNumberInput, storeLabelInput, terminalLabelInput, mobileNumberInput, timeStampInput, expireStampInput, jsonResultArea);
                    qrImageView.setImage(generateQRCodeImage(qrCodeStr, 400, 400));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    jsonResultArea.clear();
                    qrImageView.setImage(null);
                    qrStringLabel.setText("Error decoding QR.");
                }
            }
        });
    }

    private String generateQRCode(ComboBox<String> merchantTypeInput, TextField bakongAccountIDInput, TextField merchantIdInput,
                                  TextField accountInformationInput, TextField acquiringBankInput,
                                  TextField merchantNameInput, TextField merchantCityInput, TextField merchantCategoryCode,
                                  ComboBox<String> transactionCurrencyInput, TextField transactionAmountInput,
                                  TextField billNumberInput, TextField storeLabelInput, TextField terminalLabelInput,
                                  TextField mobileNumberInput, TextArea jsonResultArea) {

        String selectedCurrency = transactionCurrencyInput.getValue();
        String currencyCode = CURRENCY_MAP.get(selectedCurrency);

        String selectedMerchantType = merchantTypeInput.getValue();
        String merchantType = MERCHANT_TYPE_MAP.get(selectedMerchantType);

        if (merchantType.equalsIgnoreCase("29")) {
            IndividualInfo individualInfo = new IndividualInfo();
            individualInfo.setAccountInformation(accountInformationInput.getText());
            individualInfo.setAcquiringBank(acquiringBankInput.getText());
            individualInfo.setBakongAccountId(bakongAccountIDInput.getText());
            individualInfo.setCurrency(currencyCode.equalsIgnoreCase("KHR") ? KHQRCurrency.KHR : KHQRCurrency.USD);
            individualInfo.setMerchantName(merchantNameInput.getText());
            individualInfo.setMerchantCity(merchantCityInput.getText());
            if (StringUtils.isNotBlank(mobileNumberInput.getText())) individualInfo.setMobileNumber(mobileNumberInput.getText());
            if (StringUtils.isNotBlank(transactionAmountInput.getText())) {
                ZoneId localZone = ZoneId.systemDefault();
                // Get current local time
                LocalDateTime now = LocalDateTime.now(localZone);
                // Add 1 minute
                LocalDateTime plusOneMinute = now.plusMinutes(1);
                // Convert to milliseconds since epoch
                long milliseconds = plusOneMinute.atZone(localZone)
                        .toInstant()
                        .toEpochMilli();
                System.out.println("Current time (GMT+7): " + now);
                System.out.println("Plus 1 minute (GMT+7): " + plusOneMinute);

                individualInfo.setExpirationTimestamp(milliseconds);
                individualInfo.setAmount(Double.parseDouble(transactionAmountInput.getText()));
            }
            if (StringUtils.isNotBlank(billNumberInput.getText())) individualInfo.setBillNumber(billNumberInput.getText());

            KHQRResponse<KHQRData> response = BakongUtils.generateIndividual(individualInfo, merchantCategoryCode.getText());

            try {
                String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                jsonResultArea.setText(jsonResult);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            if (response.getKHQRStatus().getCode() == 0) {
                return response.getData().getQr();
            } else {
                throw new RuntimeException(response.getKHQRStatus().getMessage());
            }

        } else {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setBakongAccountId(bakongAccountIDInput.getText());
            merchantInfo.setAcquiringBank(acquiringBankInput.getText());
            merchantInfo.setMerchantId(merchantIdInput.getText());
            merchantInfo.setMerchantName(merchantNameInput.getText());
            merchantInfo.setMerchantCity(merchantCityInput.getText());
            merchantInfo.setCurrency(currencyCode.equalsIgnoreCase("KHR") ? KHQRCurrency.KHR : KHQRCurrency.USD);
            if (StringUtils.isNotBlank(terminalLabelInput.getText())) merchantInfo.setTerminalLabel(terminalLabelInput.getText());
            if (StringUtils.isNotBlank(storeLabelInput.getText())) merchantInfo.setStoreLabel(storeLabelInput.getText());
            if (StringUtils.isNotBlank(transactionAmountInput.getText())){
                ZoneId localZone = ZoneId.systemDefault();
                // Get current local time
                LocalDateTime now = LocalDateTime.now(localZone);
                // Add 1 minute
                LocalDateTime plusOneMinute = now.plusMinutes(1);
                // Convert to milliseconds since epoch
                long milliseconds = plusOneMinute.atZone(localZone)
                        .toInstant()
                        .toEpochMilli();
                System.out.println("Current time (GMT+7): " + now);
                System.out.println("Plus 1 minute (GMT+7): " + plusOneMinute);

                merchantInfo.setAmount(Double.parseDouble(transactionAmountInput.getText()));
                merchantInfo.setExpirationTimestamp(milliseconds);
            }
            if (StringUtils.isNotBlank(mobileNumberInput.getText())) merchantInfo.setMobileNumber(mobileNumberInput.getText());
            if (StringUtils.isNotBlank(billNumberInput.getText())) merchantInfo.setBillNumber(billNumberInput.getText());

            KHQRResponse<KHQRData> response = BakongUtils.generateMerchant(merchantInfo, merchantCategoryCode.getText());

            try {
                String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                jsonResultArea.setText(jsonResult);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            if (response.getKHQRStatus().getCode() == 0) {
                return response.getData().getQr();
            } else {
                throw new RuntimeException(response.getKHQRStatus().getMessage());
            }
        }
    }

    private String decodeQRFromFile(File file) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO.read(file);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private void updateFieldsFromDecodedQR(String qrCodeStr, Label qrStringLabel, TextField payloadFormatIndicatorInput,
                                           ComboBox<String> pointOfInitiationInput, ComboBox<String> merchantTypeInput,
                                           TextField bakongAccountIDInput, TextField merchantIdInput,
                                           TextField accountInformationInput, TextField acquiringBankInput,
                                           TextField merchantCategoryCodeInput, TextField countryCodeInput,
                                           TextField merchantNameInput, TextField merchantCityInput,
                                           ComboBox<String> transactionCurrencyInput, TextField transactionAmountInput,
                                           TextField billNumberInput, TextField storeLabelInput, TextField terminalLabelInput,
                                           TextField mobileNumberInput, Label timeStampInput, Label expireStampInput, TextArea jsonResultArea) {
        KHQRResponse<KHQRDecodeData> decode = BakongKHQR.decode(qrCodeStr);
        KHQRResponse<CRCValidation> valid = BakongKHQR.verify(qrCodeStr);

        // Convert decode object to JSON and display in jsonResultArea
        try {
            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(decode);
            jsonResultArea.setText(jsonResult);
        } catch (Exception e) {
            jsonResultArea.setText("Error serializing JSON: " + e.getMessage());
        }

        if (valid.getKHQRStatus().getCode() == 0) {
            qrStringLabel.setText("QR Code is valid");
            KHQRDecodeData data = decode.getData();
            payloadFormatIndicatorInput.setText(data.getPayloadFormatIndicator());
            pointOfInitiationInput.setValue(data.getPointOfInitiationMethod());
            merchantTypeInput.setValue(REVERSE_MERCHANT_TYPE_MAP.getOrDefault(data.getMerchantType(), "Remittance"));
            bakongAccountIDInput.setText(data.getBakongAccountID());
            merchantIdInput.setText(data.getMerchantId());
            accountInformationInput.setText(data.getAccountInformation());
            acquiringBankInput.setText(data.getAcquiringBank());
            merchantCategoryCodeInput.setText(data.getMerchantCategoryCode());
            countryCodeInput.setText(data.getCountryCode());
            merchantNameInput.setText(data.getMerchantName());
            merchantCityInput.setText(data.getMerchantCity());
            transactionCurrencyInput.setValue(REVERSE_CURRENCY_MAP.getOrDefault(data.getTransactionCurrency(), "USD"));
            transactionAmountInput.setText(data.getTransactionAmount());
            billNumberInput.setText(data.getBillNumber());
            storeLabelInput.setText(data.getStoreLabel());
            terminalLabelInput.setText(data.getTerminalLabel());
            mobileNumberInput.setText(data.getMobileNumber());
            timeStampInput.setText(StringUtils.isNotBlank(data.getTimestamp()) ? data.getTimestamp() + " - " + getUTC7DateTime(Long.parseLong(data.getTimestamp())) : "");
            expireStampInput.setText(StringUtils.isNotBlank(data.getExpirationTimestamp()) ? data.getExpirationTimestamp() + " - " + getUTC7DateTime(Long.parseLong(data.getExpirationTimestamp())) : "");
        } else {
            qrStringLabel.setText("QR Code is not valid: " + valid.getKHQRStatus().getMessage());
        }
    }

    private String getUTC7DateTime(long currentTime) {
        Instant instant = Instant.ofEpochMilli(currentTime);
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        LocalDateTime utcPlus7DateTime = utcDateTime.plusHours(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
        return utcPlus7DateTime.format(formatter);
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
        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pw.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return wr;
    }

    public static void main(String[] args) {
        launch(args);
    }
}