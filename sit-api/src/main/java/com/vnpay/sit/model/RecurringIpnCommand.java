package com.vnpay.sit.model;

/**
 * Giá trị {@code vnp_command} trong IPN Recurring theo spec VNPay.
 * <ul>
 *   <li>{@link #RECURRING} — xác thực thẻ / đăng ký (biên bản case 15–16)</li>
 *   <li>{@link #PAY_N_RECURRING} — thanh toán định kỳ</li>
 *   <li>{@link #UPDATE_TOKEN} — cập nhật token</li>
 * </ul>
 */
public enum RecurringIpnCommand {
    RECURRING("recurring", "Xác thực thẻ (recurring)"),
    PAY_N_RECURRING("pay_n_recurring", "Thanh toán định kỳ (pay_n_recurring)"),
    UPDATE_TOKEN("update_token", "Cập nhật token (update_token)");

    private final String commandValue;
    private final String label;

    RecurringIpnCommand(String commandValue, String label) {
        this.commandValue = commandValue;
        this.label = label;
    }

    public String getCommandValue() {
        return commandValue;
    }

    public String getLabel() {
        return label;
    }

    /** Mặc định cho suite IPN tự động — khớp biên bản mục “Giao dịch xác thực thẻ”. */
    public static RecurringIpnCommand defaultForIpnSuite() {
        return RECURRING;
    }
}
