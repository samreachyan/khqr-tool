package com.sakcode.decodekhqr.model;

import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;

public enum Currency {
    USD("USD", KHQRCurrency.USD),
    KHR("KHR", KHQRCurrency.KHR);

    private final String display;
    private final KHQRCurrency khqrCurrency;

    Currency(String display, KHQRCurrency khqrCurrency) {
        this.display = display;
        this.khqrCurrency = khqrCurrency;
    }

    public String display() {
        return display;
    }

    public String code() {
        return khqrCurrency.getValue();
    }

    public KHQRCurrency khqrCurrency() {
        return khqrCurrency;
    }

    public static Currency fromDisplay(String display) {
        for (Currency currency : values()) {
            if (currency.display.equalsIgnoreCase(display)) {
                return currency;
            }
        }
        return USD;
    }

    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code().equals(code)) {
                return currency;
            }
        }
        return USD;
    }
}
