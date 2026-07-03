package com.vnpay.sit.model;

public enum PaymentFlow {
    PAY("THANH TOÁN PAY"),
    TOKEN("THANH TOÁN BẰNG MÃ TOKEN"),
    RECURRING("THANH TOÁN ĐỊNH KỲ"),
    INSTALMENT("THANH TOÁN TRẢ GÓP"),
    QR_DIRECT("MERCHANT HOSTED QR (QR DIRECT)"),
    PREAUTH("THANH TOÁN ỦY QUYỀN (PRE-AUTH)");

    private final String label;

    PaymentFlow(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
