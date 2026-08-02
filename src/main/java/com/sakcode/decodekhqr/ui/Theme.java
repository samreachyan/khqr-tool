package com.sakcode.decodekhqr.ui;

/** Light/dark color themes layered on top of the shared {@link #BASE_STYLESHEET}. */
public enum Theme {

    LIGHT("/css/theme-light.css", "🌙 Dark Mode"),
    DARK("/css/theme-dark.css", "☀ Light Mode");

    public static final String BASE_STYLESHEET = "/css/base.css";

    private final String stylesheet;
    private final String toggleLabel;

    Theme(String stylesheet, String toggleLabel) {
        this.stylesheet = stylesheet;
        this.toggleLabel = toggleLabel;
    }

    public String stylesheet() {
        return stylesheet;
    }

    /** Label describing the theme this button would switch *to*. */
    public String toggleLabel() {
        return toggleLabel;
    }

    public Theme opposite() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
