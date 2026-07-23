package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Builds the JavaFX scene graph for the KHQR form. Pure layout — no event wiring, no business logic.
 */
public final class KhqrFormView {

    public record Layout(Parent root, KhqrFormFields fields, Button selectFileButton, Button decodeButton,
                          Button generateButton) {
    }

    public Layout build() {
        HBox root = new HBox(20);
        root.setPadding(new Insets(10));
        HBox.setHgrow(root, Priority.ALWAYS);

        VBox inputPanel = new VBox(10);
        inputPanel.setPrefWidth(500);
        inputPanel.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

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

        TextArea jsonResultArea = new TextArea();
        jsonResultArea.setPrefRowCount(10);
        jsonResultArea.setWrapText(true);
        jsonResultArea.setEditable(false);
        jsonResultArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(jsonResultArea, Priority.ALWAYS);
        qrDisplayBox.getChildren().addAll(qrImageView, qrStringLabel, jsonResultArea);

        FormGrid formGrid = buildFormGrid(qrImageView, qrStringLabel, jsonResultArea);
        scrollPane.setContent(formGrid.grid());
        inputPanel.getChildren().add(scrollPane);

        root.getChildren().addAll(inputPanel, qrDisplayBox);
        inputPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));

        return new Layout(root, formGrid.fields(), formGrid.selectFileButton(), formGrid.decodeButton(),
                formGrid.generateButton());
    }

    private record FormGrid(GridPane grid, KhqrFormFields fields, Button selectFileButton, Button decodeButton,
                             Button generateButton) {
    }

    private FormGrid buildFormGrid(ImageView qrImageView, Label qrStringLabel, TextArea jsonResultArea) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHgrow(Priority.SOMETIMES);
        labelColumn.setPercentWidth(40);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        inputColumn.setPercentWidth(60);
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);

        TextField payloadFormatIndicatorInput = new TextField("01");
        payloadFormatIndicatorInput.setDisable(true);

        ComboBox<String> pointOfInitiationInput = new ComboBox<>();
        pointOfInitiationInput.getItems().addAll("11", "12");
        pointOfInitiationInput.setValue("11");
        pointOfInitiationInput.setDisable(true);
        pointOfInitiationInput.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> merchantTypeInput = new ComboBox<>();
        for (MerchantType merchantType : MerchantType.values()) {
            merchantTypeInput.getItems().add(merchantType.display());
        }
        merchantTypeInput.setValue(MerchantType.REMITTANCE.display());
        merchantTypeInput.setMaxWidth(Double.MAX_VALUE);

        TextField bakongAccountIdInput = new TextField("ftcckhppxxx@ftcc");
        TextField merchantIdInput = new TextField();
        TextField accountInformationInput = new TextField("123123");
        TextField acquiringBankInput = new TextField("Foreign Trade Bank of Cambodia");
        TextField merchantCategoryCodeInput = new TextField("5999");
        TextField countryCodeInput = new TextField("KH");
        TextField merchantNameInput = new TextField("Samreach");
        TextField merchantCityInput = new TextField("PHNOM PENH");

        ComboBox<String> transactionCurrencyInput = new ComboBox<>();
        for (Currency currency : Currency.values()) {
            transactionCurrencyInput.getItems().add(currency.display());
        }
        transactionCurrencyInput.setValue(Currency.USD.display());
        transactionCurrencyInput.setMaxWidth(Double.MAX_VALUE);

        TextField transactionAmountInput = new TextField();
        TextField billNumberInput = new TextField();
        TextField storeLabelInput = new TextField();
        TextField terminalLabelInput = new TextField();
        TextField mobileNumberInput = new TextField();

        Label timeStampLabel = new Label();
        timeStampLabel.setWrapText(true);
        Label expireStampLabel = new Label();
        expireStampLabel.setWrapText(true);

        TextArea qrCodeInput = new TextArea();
        qrCodeInput.setPrefRowCount(5);
        qrCodeInput.setWrapText(true);

        Button selectFileButton = new Button("Select File");
        Button decodeQRButton = new Button("Decode QR");
        Button generateQRButton = new Button("Generate QR");

        int row = 0;
        addRow(grid, row++, "Payload Format Indicator:", payloadFormatIndicatorInput);
        addRow(grid, row++, "Point of Initiation Method:", pointOfInitiationInput);
        addRow(grid, row++, "QR Type:", merchantTypeInput);
        addRow(grid, row++, "Bakong Account ID:", bakongAccountIdInput);
        addRow(grid, row++, "Merchant ID:", merchantIdInput);
        addRow(grid, row++, "Account Information:", accountInformationInput);
        addRow(grid, row++, "Acquiring Bank:", acquiringBankInput);
        addRow(grid, row++, "Merchant Category Code:", merchantCategoryCodeInput);
        addRow(grid, row++, "Country Code:", countryCodeInput);
        addRow(grid, row++, "Merchant Name:", merchantNameInput);
        addRow(grid, row++, "Merchant City:", merchantCityInput);
        addRow(grid, row++, "Transaction Currency:", transactionCurrencyInput);
        addRow(grid, row++, "Transaction Amount:", transactionAmountInput);
        addRow(grid, row++, "Bill Number:", billNumberInput);
        addRow(grid, row++, "Store Label:", storeLabelInput);
        addRow(grid, row++, "Terminal Label:", terminalLabelInput);
        addRow(grid, row++, "Mobile Number:", mobileNumberInput);
        addRow(grid, row++, "Created at:", timeStampLabel);
        addRow(grid, row++, "Expired at:", expireStampLabel);
        addRow(grid, row++, "QR Code Input:", qrCodeInput);

        HBox buttonBox = new HBox(10, selectFileButton, decodeQRButton, generateQRButton);
        buttonBox.setAlignment(Pos.CENTER);
        GridPane.setConstraints(buttonBox, 0, row, 2, 1);
        grid.getChildren().add(buttonBox);

        KhqrFormFields fields = new KhqrFormFields(payloadFormatIndicatorInput, pointOfInitiationInput,
                merchantTypeInput, bakongAccountIdInput, merchantIdInput, accountInformationInput,
                acquiringBankInput, merchantCategoryCodeInput, countryCodeInput, merchantNameInput,
                merchantCityInput, transactionCurrencyInput, transactionAmountInput, billNumberInput,
                storeLabelInput, terminalLabelInput, mobileNumberInput, timeStampLabel, expireStampLabel,
                qrCodeInput, qrStringLabel, qrImageView, jsonResultArea);

        return new FormGrid(grid, fields, selectFileButton, decodeQRButton, generateQRButton);
    }

    private void addRow(GridPane grid, int row, String labelText, Control control) {
        Label label = new Label(labelText);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setConstraints(label, 0, row);
        GridPane.setConstraints(control, 1, row);
        grid.getChildren().addAll(label, control);
        GridPane.setHgrow(control, Priority.ALWAYS);
        if (control instanceof TextField textField) {
            textField.setPrefColumnCount(15);
        }
    }
}
