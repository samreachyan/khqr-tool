package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Builds the JavaFX scene graph for the KHQR tool: a toolbar, a Generate/Decode tab pane on the
 * left, and a shared QR preview/result panel on the right. Pure layout — no event wiring, no
 * business logic.
 */
public final class KhqrFormView {

    public record Layout(Parent root, KhqrFormFields fields, Button selectFileButton, Button decodeButton,
                          Button generateButton, Button clearButton, Button copyQrButton, Button copyJsonButton,
                          Button saveImageButton, Button themeToggleButton, Node dropZone, KhqrCardView card) {
    }

    public Layout build() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        Button themeToggleButton = new Button();
        themeToggleButton.getStyleClass().add("secondary-button");
        root.setTop(buildToolbar(themeToggleButton));

        ResultPanel resultPanel = buildResultPanel();
        GenerateTab generateTab = buildGenerateTab();
        DecodeTab decodeTab = buildDecodeTab();

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("app-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(new Tab("Generate", generateTab.content()), new Tab("Decode", decodeTab.content()));

        SplitPane splitPane = new SplitPane(tabPane, resultPanel.content());
        splitPane.setDividerPositions(0.55);
        root.setCenter(splitPane);

        KhqrFormFields fields = new KhqrFormFields(generateTab.payloadFormatIndicatorInput(),
                generateTab.pointOfInitiationInput(), generateTab.merchantTypeInput(),
                generateTab.bakongAccountIdInput(), generateTab.merchantIdInput(),
                generateTab.accountInformationInput(), generateTab.acquiringBankInput(),
                generateTab.merchantCategoryCodeInput(), generateTab.countryCodeInput(),
                generateTab.merchantNameInput(), generateTab.merchantCityInput(),
                generateTab.transactionCurrencyInput(), generateTab.transactionAmountInput(),
                generateTab.billNumberInput(), generateTab.storeLabelInput(), generateTab.terminalLabelInput(),
                generateTab.mobileNumberInput(), generateTab.timeStampLabel(), generateTab.expireStampLabel(),
                decodeTab.qrCodeInput(), resultPanel.qrStringLabel(), resultPanel.card().qrImageView(),
                resultPanel.jsonResultArea());

        return new Layout(root, fields, decodeTab.selectFileButton(), decodeTab.decodeButton(),
                generateTab.generateButton(), generateTab.clearButton(), resultPanel.copyQrButton(),
                resultPanel.copyJsonButton(), resultPanel.saveImageButton(), themeToggleButton, decodeTab.dropZone(),
                resultPanel.card());
    }

    private HBox buildToolbar(Button themeToggleButton) {
        Label title = new Label("KHQR Tool");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Generate & decode Bakong KHQR codes");
        subtitle.getStyleClass().add("app-subtitle");
        VBox titleBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(12, titleBox, spacer, themeToggleButton);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private record GenerateTab(Node content, TextField payloadFormatIndicatorInput,
                                ComboBox<String> pointOfInitiationInput, ComboBox<String> merchantTypeInput,
                                TextField bakongAccountIdInput, TextField merchantIdInput,
                                TextField accountInformationInput, TextField acquiringBankInput,
                                TextField merchantCategoryCodeInput, TextField countryCodeInput,
                                TextField merchantNameInput, TextField merchantCityInput,
                                ComboBox<String> transactionCurrencyInput, TextField transactionAmountInput,
                                TextField billNumberInput, TextField storeLabelInput, TextField terminalLabelInput,
                                TextField mobileNumberInput, Label timeStampLabel, Label expireStampLabel,
                                Button generateButton, Button clearButton) {
    }

    private GenerateTab buildGenerateTab() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setVgap(10);
        grid.setHgap(12);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(40);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        inputColumn.setPercentWidth(60);
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);

        TextField payloadFormatIndicatorInput = new TextField(FormDefaults.PAYLOAD_FORMAT_INDICATOR);
        payloadFormatIndicatorInput.setDisable(true);

        ComboBox<String> pointOfInitiationInput = new ComboBox<>();
        pointOfInitiationInput.getItems().addAll("11", "12");
        pointOfInitiationInput.setValue(FormDefaults.POINT_OF_INITIATION);
        pointOfInitiationInput.setDisable(true);
        pointOfInitiationInput.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> merchantTypeInput = new ComboBox<>();
        for (MerchantType merchantType : MerchantType.values()) {
            merchantTypeInput.getItems().add(merchantType.display());
        }
        merchantTypeInput.setValue(MerchantType.REMITTANCE.display());
        merchantTypeInput.setMaxWidth(Double.MAX_VALUE);

        TextField bakongAccountIdInput = new TextField(FormDefaults.BAKONG_ACCOUNT_ID);
        TextField merchantIdInput = new TextField();
        TextField accountInformationInput = new TextField(FormDefaults.ACCOUNT_INFORMATION);
        TextField acquiringBankInput = new TextField(FormDefaults.ACQUIRING_BANK);
        TextField merchantCategoryCodeInput = new TextField(FormDefaults.MERCHANT_CATEGORY_CODE);
        TextField countryCodeInput = new TextField(FormDefaults.COUNTRY_CODE);
        TextField merchantNameInput = new TextField(FormDefaults.MERCHANT_NAME);
        TextField merchantCityInput = new TextField(FormDefaults.MERCHANT_CITY);

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

        int row = 0;
        addRow(grid, row++, "QR Type", merchantTypeInput);
        addRow(grid, row++, "Payload Format Indicator", payloadFormatIndicatorInput);
        addRow(grid, row++, "Point of Initiation Method", pointOfInitiationInput);
        addRow(grid, row++, "Bakong Account ID", bakongAccountIdInput);
        addRow(grid, row++, "Merchant ID", merchantIdInput);
        addRow(grid, row++, "Account Information", accountInformationInput);
        addRow(grid, row++, "Acquiring Bank", acquiringBankInput);
        addRow(grid, row++, "Merchant Category Code", merchantCategoryCodeInput);
        addRow(grid, row++, "Country Code", countryCodeInput);
        addRow(grid, row++, "Merchant Name", merchantNameInput);
        addRow(grid, row++, "Merchant City", merchantCityInput);
        addRow(grid, row++, "Transaction Currency", transactionCurrencyInput);
        addRow(grid, row++, "Transaction Amount", transactionAmountInput);
        addRow(grid, row++, "Bill Number", billNumberInput);
        addRow(grid, row++, "Store Label", storeLabelInput);
        addRow(grid, row++, "Terminal Label", terminalLabelInput);
        addRow(grid, row++, "Mobile Number", mobileNumberInput);
        addRow(grid, row++, "Created At", timeStampLabel);
        addRow(grid, row++, "Expires At", expireStampLabel);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button generateButton = new Button("Generate QR");
        generateButton.getStyleClass().add("primary-button");
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("secondary-button");
        HBox buttonRow = new HBox(10, clearButton, generateButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getStyleClass().add("button-row");

        VBox content = new VBox(12, scrollPane, buttonRow);
        content.getStyleClass().add("tab-content");

        return new GenerateTab(content, payloadFormatIndicatorInput, pointOfInitiationInput, merchantTypeInput,
                bakongAccountIdInput, merchantIdInput, accountInformationInput, acquiringBankInput,
                merchantCategoryCodeInput, countryCodeInput, merchantNameInput, merchantCityInput,
                transactionCurrencyInput, transactionAmountInput, billNumberInput, storeLabelInput,
                terminalLabelInput, mobileNumberInput, timeStampLabel, expireStampLabel, generateButton,
                clearButton);
    }

    private record DecodeTab(Node content, Node dropZone, TextArea qrCodeInput, Button selectFileButton,
                              Button decodeButton) {
    }

    private DecodeTab buildDecodeTab() {
        Label dropIcon = new Label("⬆");
        dropIcon.getStyleClass().add("drop-zone-icon");
        Label dropLabel = new Label("Drag & drop a QR image here");
        dropLabel.getStyleClass().add("drop-zone-label");
        Label dropHint = new Label("PNG / JPG — or use Select File below");
        dropHint.getStyleClass().add("drop-zone-hint");

        VBox dropZone = new VBox(6, dropIcon, dropLabel, dropHint);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.getStyleClass().add("drop-zone");

        Button selectFileButton = new Button("Select File...");
        selectFileButton.getStyleClass().add("secondary-button");
        HBox selectRow = new HBox(selectFileButton);
        selectRow.setAlignment(Pos.CENTER);

        Label qrTextLabel = new Label("Or paste / edit the raw KHQR text:");
        qrTextLabel.getStyleClass().add("field-label");
        TextArea qrCodeInput = new TextArea();
        qrCodeInput.setPrefRowCount(6);
        qrCodeInput.setWrapText(true);
        VBox.setVgrow(qrCodeInput, Priority.ALWAYS);

        Button decodeButton = new Button("Decode QR");
        decodeButton.getStyleClass().add("primary-button");
        HBox buttonRow = new HBox(decodeButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getStyleClass().add("button-row");

        VBox content = new VBox(16, dropZone, selectRow, qrTextLabel, qrCodeInput, buttonRow);
        content.getStyleClass().add("tab-content");
        VBox.setVgrow(content, Priority.ALWAYS);

        return new DecodeTab(content, dropZone, qrCodeInput, selectFileButton, decodeButton);
    }

    private record ResultPanel(Node content, KhqrCardView card, Label qrStringLabel, TextArea jsonResultArea,
                                Button copyQrButton, Button copyJsonButton, Button saveImageButton) {
    }

    private ResultPanel buildResultPanel() {
        Label header = new Label("QR Preview");
        header.getStyleClass().add("panel-header");

        KhqrCardView card = new KhqrCardView(260);

        VBox imageBox = new VBox(card);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.getStyleClass().add("image-box");

        Button copyQrButton = new Button("Copy QR Text");
        copyQrButton.getStyleClass().add("icon-button");
        Button copyJsonButton = new Button("Copy JSON");
        copyJsonButton.getStyleClass().add("icon-button");
        Button saveImageButton = new Button("Save Image");
        saveImageButton.getStyleClass().add("icon-button");
        HBox actionRow = new HBox(8, copyQrButton, copyJsonButton, saveImageButton);
        actionRow.setAlignment(Pos.CENTER);

        Label qrStringLabel = new Label();
        qrStringLabel.setWrapText(true);
        qrStringLabel.setMaxWidth(Double.MAX_VALUE);
        qrStringLabel.getStyleClass().add("status-label");

        Label jsonHeader = new Label("Response JSON");
        jsonHeader.getStyleClass().add("field-label");
        TextArea jsonResultArea = new TextArea();
        jsonResultArea.setEditable(false);
        jsonResultArea.setWrapText(true);
        jsonResultArea.setPrefRowCount(10);
        VBox.setVgrow(jsonResultArea, Priority.ALWAYS);

        VBox content = new VBox(12, header, imageBox, actionRow, qrStringLabel, jsonHeader, jsonResultArea);
        content.getStyleClass().addAll("tab-content", "result-panel");
        VBox.setVgrow(content, Priority.ALWAYS);

        return new ResultPanel(content, card, qrStringLabel, jsonResultArea, copyQrButton, copyJsonButton,
                saveImageButton);
    }

    private void addRow(GridPane grid, int row, String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
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
