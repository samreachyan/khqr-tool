package com.sakcode.decodekhqr.model;

import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;

public enum Currency {
    USD("USD", 2, KHQRCurrency.USD),
    KHR("KHR", 0, KHQRCurrency.KHR);

    private final String display;
    private final int minorUnits;
    private final KHQRCurrency khqrCurrency;

    Currency(String display, int minorUnits, KHQRCurrency khqrCurrency) {
        this.display = display;
        this.minorUnits = minorUnits;
        this.khqrCurrency = khqrCurrency;
    }

    public String display() {
        return display;
    }

    /** Decimal places the currency subdivides into: cents for USD, none for riel. */
    public int minorUnits() {
        return minorUnits;
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
