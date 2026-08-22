package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.model.MerchantType;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Validates required KHQR generation fields and flags invalid controls with the {@code field-error} style. */
public final class FormValidator {

    static final String ERROR_CLASS = "field-error";

    private FormValidator() {
    }

    public static List<String> validate(KhqrFormFields fields, MerchantType merchantType) {
        clearErrors(fields);
        List<String> errors = new ArrayList<>();

        requireText(fields.bakongAccountIdInput(), "Bakong Account ID", errors);
        requireText(fields.acquiringBankInput(), "Acquiring Bank", errors);
        requireText(fields.merchantCategoryCodeInput(), "Merchant Category Code", errors);
        requireText(fields.merchantNameInput(), "Merchant Name", errors);
        requireText(fields.merchantCityInput(), "Merchant City", errors);

        if (merchantType.isIndividual()) {
            requireText(fields.accountInformationInput(), "Account Information", errors);
        } else {
            requireText(fields.merchantIdInput(), "Merchant ID", errors);
        }

        String amountText = fields.transactionAmountInput().getText();
        if (StringUtils.isNotBlank(amountText) && !isNumeric(amountText)) {
            markError(fields.transactionAmountInput());
            errors.add("Transaction Amount must be a valid number");
        }

        return errors;
    }

    public static void clearErrors(KhqrFormFields fields) {
        for (Control control : requiredControls(fields)) {
            control.getStyleClass().remove(ERROR_CLASS);
        }
    }

    private static void requireText(TextField field, String label, List<String> errors) {
        if (StringUtils.isBlank(field.getText())) {
            markError(field);
            errors.add(label + " is required");
        }
    }

    private static boolean isNumeric(String text) {
        try {
            Double.parseDouble(text.replace(",", "").trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void markError(Control control) {
        if (!control.getStyleClass().contains(ERROR_CLASS)) {
            control.getStyleClass().add(ERROR_CLASS);
        }
    }

    private static List<Control> requiredControls(KhqrFormFields fields) {
        return List.of(fields.bakongAccountIdInput(), fields.acquiringBankInput(),
                fields.merchantCategoryCodeInput(), fields.merchantNameInput(), fields.merchantCityInput(),
                fields.accountInformationInput(), fields.merchantIdInput(), fields.transactionAmountInput());
    }
}
