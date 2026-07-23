package com.sakcode.decodekhqr.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public record KhqrFormFields(
        TextField payloadFormatIndicatorInput,
        ComboBox<String> pointOfInitiationInput,
        ComboBox<String> merchantTypeInput,
        TextField bakongAccountIdInput,
        TextField merchantIdInput,
        TextField accountInformationInput,
        TextField acquiringBankInput,
        TextField merchantCategoryCodeInput,
        TextField countryCodeInput,
        TextField merchantNameInput,
        TextField merchantCityInput,
        ComboBox<String> transactionCurrencyInput,
        TextField transactionAmountInput,
        TextField billNumberInput,
        TextField storeLabelInput,
        TextField terminalLabelInput,
        TextField mobileNumberInput,
        Label timeStampLabel,
        Label expireStampLabel,
        TextArea qrCodeInput,
        Label qrStringLabel,
        ImageView qrImageView,
        TextArea jsonResultArea) {
}
