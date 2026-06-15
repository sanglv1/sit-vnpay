package com.vnpay.sit.model;

public enum CallbackType {
    RETURN("Return URL"),
    IPN("IPN URL");

    private final String label;

    CallbackType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
