package com.sakcode.decodekhqr.model;

import kh.gov.nbc.bakong_khqr.model.KHQRMerchantType;

public enum MerchantType {
    REMITTANCE("Remittance", KHQRMerchantType.INDIVIDUAL),
    MERCHANT("Merchant", KHQRMerchantType.MERCHANT);

    private final String display;
    private final KHQRMerchantType khqrMerchantType;

    MerchantType(String display, KHQRMerchantType khqrMerchantType) {
        this.display = display;
        this.khqrMerchantType = khqrMerchantType;
    }

    public String display() {
        return display;
    }

    public String code() {
        return khqrMerchantType.getType();
    }

    public boolean isIndividual() {
        return this == REMITTANCE;
    }

    public static MerchantType fromDisplay(String display) {
        for (MerchantType merchantType : values()) {
            if (merchantType.display.equalsIgnoreCase(display)) {
                return merchantType;
            }
        }
        return REMITTANCE;
    }

    public static MerchantType fromCode(String code) {
        for (MerchantType merchantType : values()) {
            if (merchantType.code().equals(code)) {
                return merchantType;
            }
        }
        return REMITTANCE;
    }
}
