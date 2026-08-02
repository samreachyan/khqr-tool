package com.sakcode.decodekhqr.khqr;

import com.sakcode.decodekhqr.model.Currency;
import com.sakcode.decodekhqr.model.MerchantType;
import com.sakcode.decodekhqr.ui.KhqrFormFields;
import com.sakcode.decodekhqr.util.Timestamps;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRDecodeData;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import org.apache.commons.lang3.StringUtils;

/** Translates between {@link KhqrFormFields} (JavaFX) and the KHQR SDK model classes. */
public final class KhqrFormMapper {

    private KhqrFormMapper() {
    }

    public static IndividualInfo toIndividualInfo(KhqrFormFields fields, Currency currency) {
        IndividualInfo individualInfo = new IndividualInfo();
        individualInfo.setAccountInformation(fields.accountInformationInput().getText());
        individualInfo.setAcquiringBank(fields.acquiringBankInput().getText());
        individualInfo.setBakongAccountId(fields.bakongAccountIdInput().getText());
        individualInfo.setCurrency(currency.khqrCurrency());
        individualInfo.setMerchantName(fields.merchantNameInput().getText());
        individualInfo.setMerchantCity(fields.merchantCityInput().getText());

        if (StringUtils.isNotBlank(fields.mobileNumberInput().getText())) {
            individualInfo.setMobileNumber(fields.mobileNumberInput().getText());
        }
        String amountText = fields.transactionAmountInput().getText();
        if (StringUtils.isNotBlank(amountText)) {
            individualInfo.setExpirationTimestamp(Timestamps.tenMinutesFromNowMillis());
            individualInfo.setAmount(Double.parseDouble(amountText));
        }
        if (StringUtils.isNotBlank(fields.billNumberInput().getText())) {
            individualInfo.setBillNumber(fields.billNumberInput().getText());
        }

        return individualInfo;
    }

    public static MerchantInfo toMerchantInfo(KhqrFormFields fields, Currency currency) {
        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setBakongAccountId(fields.bakongAccountIdInput().getText());
        merchantInfo.setAcquiringBank(fields.acquiringBankInput().getText());
        merchantInfo.setMerchantId(fields.merchantIdInput().getText());
        merchantInfo.setMerchantName(fields.merchantNameInput().getText());
        merchantInfo.setMerchantCity(fields.merchantCityInput().getText());
        merchantInfo.setCurrency(currency.khqrCurrency());

        if (StringUtils.isNotBlank(fields.terminalLabelInput().getText())) {
            merchantInfo.setTerminalLabel(fields.terminalLabelInput().getText());
        }
        if (StringUtils.isNotBlank(fields.storeLabelInput().getText())) {
            merchantInfo.setStoreLabel(fields.storeLabelInput().getText());
        }
        String amountText = fields.transactionAmountInput().getText();
        if (StringUtils.isNotBlank(amountText)) {
            merchantInfo.setAmount(Double.parseDouble(amountText));
            merchantInfo.setExpirationTimestamp(Timestamps.tenMinutesFromNowMillis());
        }
        if (StringUtils.isNotBlank(fields.mobileNumberInput().getText())) {
            merchantInfo.setMobileNumber(fields.mobileNumberInput().getText());
        }
        if (StringUtils.isNotBlank(fields.billNumberInput().getText())) {
            merchantInfo.setBillNumber(fields.billNumberInput().getText());
        }

        return merchantInfo;
    }

    public static void applyDecodedData(KHQRDecodeData data, KhqrFormFields fields) {
        fields.merchantTypeInput().setValue(MerchantType.fromCode(data.getMerchantType()).display());
        fields.bakongAccountIdInput().setText(data.getBakongAccountID());
        fields.merchantIdInput().setText(data.getMerchantId());
        fields.accountInformationInput().setText(data.getAccountInformation());
        fields.acquiringBankInput().setText(data.getAcquiringBank());
        fields.merchantCategoryCodeInput().setText(data.getMerchantCategoryCode());
        fields.countryCodeInput().setText(data.getCountryCode());
        fields.merchantNameInput().setText(data.getMerchantName());
        fields.merchantCityInput().setText(data.getMerchantCity());
        fields.transactionCurrencyInput().setValue(Currency.fromCode(data.getTransactionCurrency()).display());
        fields.transactionAmountInput().setText(data.getTransactionAmount());
        fields.billNumberInput().setText(data.getBillNumber());
        fields.storeLabelInput().setText(data.getStoreLabel());
        fields.terminalLabelInput().setText(data.getTerminalLabel());
        fields.mobileNumberInput().setText(data.getMobileNumber());
        fields.timeStampLabel().setText(Timestamps.labelFor(data.getTimestamp()));
        fields.expireStampLabel().setText(Timestamps.labelFor(data.getExpirationTimestamp()));
    }
}
