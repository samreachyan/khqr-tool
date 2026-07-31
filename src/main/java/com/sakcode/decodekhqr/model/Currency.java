package com.sakcode.decodekhqr.model;

import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;

public enum Currency {
    USD("USD", "$", KHQRCurrency.USD),
    KHR("KHR", "៛", KHQRCurrency.KHR);

    private final String display;
    private final String symbol;
    private final KHQRCurrency khqrCurrency;

    Currency(String display, String symbol, KHQRCurrency khqrCurrency) {
        this.display = display;
        this.symbol = symbol;
        this.khqrCurrency = khqrCurrency;
    }

    public String display() {
        return display;
    }

    /** Currency symbol, per the KHQR Card Guideline's "Symbol usage" section. */
    public String symbol() {
        return symbol;
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
