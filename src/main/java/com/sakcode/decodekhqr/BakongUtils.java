package com.sakcode.decodekhqr;

import kh.gov.nbc.bakong_khqr.exception.KHQRException;
import kh.gov.nbc.bakong_khqr.model.*;
import kh.gov.nbc.bakong_khqr.presenter.GenerateDeepLinkPresenter;
import kh.gov.nbc.bakong_khqr.presenter.MerchantPresentedDecodeMode;
import kh.gov.nbc.bakong_khqr.presenter.MerchantPresentedMode;
import kh.gov.nbc.bakong_khqr.utils.BakongKHQRUtils;
import kh.gov.nbc.bakong_khqr.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BakongUtils {
    private static final String PAYLOAD_FORMAT_INDICATOR = "01";
    private static final String COUNTRY_CODE = "KH";


    public BakongUtils() {
    }

    public static KHQRResponse<KHQRData> generateIndividual(IndividualInfo individualInfo, String merchantCategoryCode) {
        KHQRResponse<KHQRData> khqrResponse = new KHQRResponse();
        KHQRStatus KHQRStatus = new KHQRStatus();

        try {
            KHQRData khqrData = new KHQRData();
            String qr = getIndividualQr(individualInfo, merchantCategoryCode);
            khqrData.setQr(qr);
            khqrData.setMd5(hashTextToMd5(qr));
            khqrResponse.setData(khqrData);
        } catch (KHQRException exception) {
            KHQRStatus.setCode(Constant.ERROR_CODE);
            KHQRStatus.setErrorCode(exception.getErrorCode());
            KHQRStatus.setMessage(exception.getMessage());
        }

        khqrResponse.setKHQRStatus(KHQRStatus);
        return khqrResponse;
    }

    public static KHQRResponse<KHQRData> generateMerchant(MerchantInfo merchantInfo, String merchantCategoryCode) {
        KHQRResponse<KHQRData> khqrResponse = new KHQRResponse();
        KHQRStatus KHQRStatus = new KHQRStatus();

        try {
            KHQRData khqrData = new KHQRData();
            String qr = getMerchantQr(merchantInfo, merchantCategoryCode);
            khqrData.setQr(qr);
            khqrData.setMd5(hashTextToMd5(qr));
            khqrResponse.setData(khqrData);
        } catch (KHQRException exception) {
            KHQRStatus.setCode(Constant.ERROR_CODE);
            KHQRStatus.setErrorCode(exception.getErrorCode());
            KHQRStatus.setMessage(exception.getMessage());
        }

        khqrResponse.setKHQRStatus(KHQRStatus);
        return khqrResponse;
    }

    public static KHQRResponse<CRCValidation> verify(String khqrCode) {
        KHQRStatus khqrStatus = new KHQRStatus();
        KHQRResponse<CRCValidation> khqrResponse = new KHQRResponse();
        if (!StringUtils.isBlank(khqrCode) && khqrCode.trim().length() >= 8) {
            KHQRResponse<KHQRDecodeData> khqrDecodeData = decodeToVerify(khqrCode);
            CRCValidation crcValidation = new CRCValidation();
            if (khqrDecodeData.getKHQRStatus().getCode() == Constant.SUCCESS_CODE) {
                khqrResponse.setKHQRStatus(khqrStatus);
                khqrCode = khqrCode.trim();
                String checksum = khqrCode.substring(khqrCode.length() - 4).toUpperCase();
                String dataPayload = khqrCode.substring(0, khqrCode.length() - 4);
                crcValidation.setValid(checksum.equals(BakongKHQRUtils.getChecksumResult(dataPayload)));
            } else {
                khqrResponse.setKHQRStatus(khqrDecodeData.getKHQRStatus());
                crcValidation.setValid(false);
            }

            khqrResponse.setData(crcValidation);
        } else {
            setStatusInvalidQr(khqrStatus, khqrResponse);
        }

        return khqrResponse;
    }

    public static KHQRResponse<KHQRDeepLinkData> generateDeepLink(String url, String qr, SourceInfo sourceInfo) {
        if (!KHQRValidation.isEmpty(url) && !KHQRValidation.isUrlInvalid(url)) {
            KHQRResponse<CRCValidation> crcValidation = verify(qr);
            if (crcValidation.getKHQRStatus().getCode() != Constant.ERROR_CODE && ((CRCValidation)crcValidation.getData()).isValid()) {
                if (KHQRValidation.isSourceInfoInvalid(sourceInfo)) {
                    return GenerateDeepLinkPresenter.responseError(14);
                } else {
                    try {
                        return (new GenerateDeepLinkPresenter(url, qr, sourceInfo)).generate();
                    } catch (KHQRException exception) {
                        return GenerateDeepLinkPresenter.responseError(exception.getErrorCode());
                    }
                }
            } else {
                KHQRStatus status = crcValidation.getKHQRStatus();
                return status.getErrorCode() != null && status.getMessage() != null ? GenerateDeepLinkPresenter.responseError(status.getErrorCode()) : GenerateDeepLinkPresenter.responseError(8);
            }
        } else {
            return GenerateDeepLinkPresenter.responseError(29);
        }
    }

    public static KHQRResponse<KHQRDecodeData> decode(String qr) {
        KHQRResponse<KHQRDecodeData> khqrResponse = new KHQRResponse();
        KHQRStatus khqrStatus = new KHQRStatus();

        try {
            KHQRDecodeData khqrDecodeData = (new MerchantPresentedDecodeMode()).decode(qr, false);
            khqrResponse.setKHQRStatus(khqrStatus);
            khqrResponse.setData(khqrDecodeData);
        } catch (KHQRException e) {
            e.printStackTrace();
        }

        return khqrResponse;
    }

    private static KHQRResponse<KHQRDecodeData> decodeToVerify(String qr) {
        KHQRResponse<KHQRDecodeData> khqrResponse = new KHQRResponse();
        KHQRStatus khqrStatus = new KHQRStatus();
        if (!StringUtils.isBlank(qr) && qr.trim().length() >= 8) {
            try {
                KHQRDecodeData khqrDecodeData = (new MerchantPresentedDecodeMode()).decode(qr, true);
                khqrResponse.setKHQRStatus(khqrStatus);
                khqrResponse.setData(khqrDecodeData);
            } catch (KHQRException e) {
                khqrStatus.setCode(Constant.ERROR_CODE);
                khqrStatus.setErrorCode(e.getErrorCode());
                khqrStatus.setMessage(e.getMessage());
                khqrResponse.setKHQRStatus(khqrStatus);
            }
        } else {
            setInvalidQrData(khqrResponse, khqrStatus);
        }

        return khqrResponse;
    }

    private static void setInvalidQrData(KHQRResponse<KHQRDecodeData> khqrResponse, KHQRStatus khqrStatus) {
        khqrStatus.setErrorCode(8);
        khqrStatus.setCode(Constant.ERROR_CODE);
        khqrStatus.setMessage((String)KHQRErrorCode.errorCodeMap.get(8));
        khqrResponse.setKHQRStatus(khqrStatus);
    }

    private static void setStatusInvalidQr(KHQRStatus khqrStatus, KHQRResponse<CRCValidation> khqrResponse) {
        khqrStatus.setErrorCode(8);
        khqrStatus.setCode(Constant.ERROR_CODE);
        khqrStatus.setMessage((String)KHQRErrorCode.errorCodeMap.get(8));
        khqrResponse.setKHQRStatus(khqrStatus);
    }

    private static String getMerchantQr(MerchantInfo merchantInfo, String merchantCategoryCode) throws KHQRException {
        KHQRValidation.validate(merchantInfo);
        MerchantPresentedMode merchantPresentedMode = new MerchantPresentedMode();
        merchantPresentedMode.addMerchantAccountInformation(getMerchantAccountInfo(merchantInfo));
        merchantPresentedMode.setAdditionalDataField(getMerchantAdditionalData(merchantInfo));
        merchantPresentedMode.setMerchantInformationLanguage(getMerchantInformationLanguage(merchantInfo));
        return getMerchantPresentedMode(merchantPresentedMode, merchantInfo.getMerchantName(), merchantInfo.getAmount(), merchantInfo.getCurrency(), merchantInfo.getMerchantCity(), merchantInfo.getUpiAccountInformation(), merchantInfo.getExpirationTimestamp(), merchantCategoryCode);
    }

    private static String getIndividualQr(IndividualInfo individualInfo, String merchantCategoryCode) throws KHQRException {
        KHQRValidation.validate(individualInfo);
        MerchantPresentedMode merchantPresentedMode = new MerchantPresentedMode();
        merchantPresentedMode.addMerchantAccountInformation(getIndividualAccountInfo(individualInfo));
        merchantPresentedMode.setAdditionalDataField(getIndividualAdditionalData(individualInfo));
        merchantPresentedMode.setMerchantInformationLanguage(getMerchantInformationLanguage(individualInfo));
        return getMerchantPresentedMode(merchantPresentedMode, individualInfo.getMerchantName(), individualInfo.getAmount(), individualInfo.getCurrency(), individualInfo.getMerchantCity(), individualInfo.getUpiAccountInformation(), individualInfo.getExpirationTimestamp(), merchantCategoryCode);
    }

    private static String getMerchantPresentedMode(MerchantPresentedMode merchantPresentedMode, String merchantName, Double amount, KHQRCurrency currency, String merchantCity, String upiAccountInformation, Long expirationTimestamp, String merchantCategoryCode) throws KHQRException {
        merchantPresentedMode.setMerchantName(merchantName);
        merchantPresentedMode.setTransactionCurrency(currency.getValue());
        if (amount != null) {
            BigDecimal bd = BigDecimal.valueOf(amount);
            merchantPresentedMode.setTransactionAmount(bd.stripTrailingZeros().toPlainString());
        }

        KHQRType qrType = KHQRType.getByAmount(amount);
        merchantPresentedMode.setMerchantCity(merchantCity);
        merchantPresentedMode.setCountryCode(COUNTRY_CODE);
        merchantPresentedMode.setPayloadFormatIndicator(PAYLOAD_FORMAT_INDICATOR);
        merchantPresentedMode.setUpiAccountInformation(upiAccountInformation);
        merchantPresentedMode.setPointOfInitiationMethod(qrType.getValue());
        merchantPresentedMode.setMerchantCategoryCode(merchantCategoryCode);
        if (KHQRType.DYNAMIC.equals(qrType)) {
            merchantPresentedMode.setTimestamps(expirationTimestamp);
        }

        return merchantPresentedMode.toString();
    }

    private static MerchantAccountInformationTemplate getMerchantAccountInfo(MerchantInfo merchantInfo) throws KHQRException {
        MerchantAccountInformation merchantAccountInformationValue = new MerchantAccountInformation();
        merchantAccountInformationValue.setGloballyUniqueIdentifier(merchantInfo.getBakongAccountId());
        merchantAccountInformationValue.setMerchantId(merchantInfo.getMerchantId());
        merchantAccountInformationValue.setAcquiringBank(merchantInfo.getAcquiringBank());
        MerchantAccountInformationTemplate merchantAccountInformation = new MerchantAccountInformationTemplate();
        merchantAccountInformation.setTag(KHQRMerchantType.MERCHANT.getType());
        merchantAccountInformation.setValue(merchantAccountInformationValue);
        return merchantAccountInformation;
    }

    private static MerchantAccountInformationTemplate getIndividualAccountInfo(IndividualInfo individualInfo) throws KHQRException {
        MerchantAccountInformation merchantAccountInformationValue = new MerchantAccountInformation();
        merchantAccountInformationValue.setGloballyUniqueIdentifier(individualInfo.getBakongAccountId());
        merchantAccountInformationValue.setAccountInformation(individualInfo.getAccountInformation());
        merchantAccountInformationValue.setAcquiringBank(individualInfo.getAcquiringBank());
        MerchantAccountInformationTemplate merchantAccountInformation = new MerchantAccountInformationTemplate();
        merchantAccountInformation.setTag(KHQRMerchantType.INDIVIDUAL.getType());
        merchantAccountInformation.setValue(merchantAccountInformationValue);
        return merchantAccountInformation;
    }

    private static MerchantInformationLanguageTemplate getMerchantInformationLanguage(MerchantInfo merchantInfo) {
        MerchantInformationLanguage merchantInformationLanguage = new MerchantInformationLanguage();
        merchantInformationLanguage.setLanguagePreference(merchantInfo.getMerchantAlternateLanguagePreference());
        merchantInformationLanguage.setMerchantCity(merchantInfo.getMerchantCityAlternateLanguage());
        merchantInformationLanguage.setMerchantName(merchantInfo.getMerchantNameAlternateLanguage());
        MerchantInformationLanguageTemplate merchantInformationLanguageTemplate = new MerchantInformationLanguageTemplate();
        merchantInformationLanguageTemplate.setMerchantInformationLanguage(merchantInformationLanguage);
        return merchantInformationLanguageTemplate;
    }

    private static MerchantInformationLanguageTemplate getMerchantInformationLanguage(IndividualInfo individualInfo) {
        MerchantInformationLanguage merchantInformationLanguage = new MerchantInformationLanguage();
        merchantInformationLanguage.setLanguagePreference(individualInfo.getMerchantAlternateLanguagePreference());
        merchantInformationLanguage.setMerchantCity(individualInfo.getMerchantCityAlternateLanguage());
        merchantInformationLanguage.setMerchantName(individualInfo.getMerchantNameAlternateLanguage());
        MerchantInformationLanguageTemplate merchantInformationLanguageTemplate = new MerchantInformationLanguageTemplate();
        merchantInformationLanguageTemplate.setMerchantInformationLanguage(merchantInformationLanguage);
        return merchantInformationLanguageTemplate;
    }

    private static AdditionalDataFieldTemplate getMerchantAdditionalData(MerchantInfo merchantInfo) throws KHQRException {
        return getAdditionalDataFieldTemplate(merchantInfo.getTerminalLabel(), merchantInfo.getStoreLabel(), merchantInfo.getBillNumber(), merchantInfo.getMobileNumber(), merchantInfo.getPurposeOfTransaction());
    }

    private static AdditionalDataFieldTemplate getIndividualAdditionalData(IndividualInfo individualInfo) throws KHQRException {
        return getAdditionalDataFieldTemplate(individualInfo.getTerminalLabel(), individualInfo.getStoreLabel(), individualInfo.getBillNumber(), individualInfo.getMobileNumber(), individualInfo.getPurposeOfTransaction());
    }

    private static AdditionalDataFieldTemplate getAdditionalDataFieldTemplate(String terminalLabel, String storeLabel, String billNumber, String mobileNumber, String purposeOfTransaction) throws KHQRException {
        AdditionalDataField additionalDataField = new AdditionalDataField();
        additionalDataField.setTerminalLabel(terminalLabel);
        additionalDataField.setStoreLabel(storeLabel);
        additionalDataField.setBillNumber(billNumber);
        additionalDataField.setMobileNumber(mobileNumber);
        additionalDataField.setPurposeOfTransaction(purposeOfTransaction);
        AdditionalDataFieldTemplate additionalDataFieldTemplate = new AdditionalDataFieldTemplate();
        additionalDataFieldTemplate.setValue(additionalDataField);
        return additionalDataFieldTemplate;
    }

    private static String hashTextToMd5(String qr) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(qr.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            StringBuilder hashText = new StringBuilder(bigInt.toString(16));

            while(hashText.length() < 32) {
                hashText.insert(0, "0");
            }

            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}
