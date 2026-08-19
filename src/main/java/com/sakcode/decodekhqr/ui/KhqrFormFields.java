package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.history.HistoryEntry;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public record KhqrFormFields(
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
        TextArea jsonResultArea,
        Button scanCameraButton,
        Button copyImageButton,
        Button printSheetButton,
        Label expiryCountdownLabel,
        ListView<HistoryEntry> historyListView) {
}
