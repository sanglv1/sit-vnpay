package com.vnpay.sit.model;

/**
 * Giá trị {@code vnp_command} trong IPN/Return Token theo spec VNPay 2.1.0.
 */
public enum TokenIpnCommand {
    TOKEN_CREATE("token_create", "Tạo Token (token_create)"),
    PAY_AND_CREATE("pay_and_create", "Thanh toán và tạo Token (pay_and_create)"),
    TOKEN_PAY("token_pay", "Thanh toán bằng Token (token_pay)"),
    TOKEN_REMOVE("token_remove", "Xóa Token (token_remove)");

    private final String commandValue;
    private final String label;

    TokenIpnCommand(String commandValue, String label) {
        this.commandValue = commandValue;
        this.label = label;
    }

    public String getCommandValue() {
        return commandValue;
    }

    public String getLabel() {
        return label;
    }

    /** Mặc định suite IPN — khớp biên bản “Thanh toán và tạo Token” và đơn pending OTP thường gặp. */
    public static TokenIpnCommand defaultForIpnSuite() {
        return PAY_AND_CREATE;
    }

    public boolean includesPaymentAmount() {
        return this == PAY_AND_CREATE || this == TOKEN_PAY;
    }
}
