package com.vnpay.sit.model;

public enum PaymentFlow {
    PAY("Thanh toán PAY"),
    TOKEN("Token"),
    RECURRING("Recurring"),
    INSTALMENT("Trả góp");

    private final String label;

    PaymentFlow(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
