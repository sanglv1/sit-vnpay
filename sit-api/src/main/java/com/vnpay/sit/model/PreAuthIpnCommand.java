package com.vnpay.sit.model;

/**
 * Giá trị {@code vnp_command} trong IPN/Return PreAuth (pa-svc) theo spec VNPay 2.1.0.
 */
public enum PreAuthIpnCommand {
    CREATE_TOKEN("create_token", "Tạo Token / Ủy quyền lần đầu (create_token)"),
    AUTH_W_TOKEN("auth_w_token", "Ủy quyền bằng Token (auth_w_token)");

    private final String commandValue;
    private final String label;

    PreAuthIpnCommand(String commandValue, String label) {
        this.commandValue = commandValue;
        this.label = label;
    }

    public String getCommandValue() {
        return commandValue;
    }

    public String getLabel() {
        return label;
    }

    /** Mặc định suite IPN — khớp luồng init create_token sau khi khách nhập thẻ. */
    public static PreAuthIpnCommand defaultForIpnSuite() {
        return CREATE_TOKEN;
    }
}
