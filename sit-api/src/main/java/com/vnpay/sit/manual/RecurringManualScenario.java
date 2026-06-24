package com.vnpay.sit.manual;

import java.util.Arrays;
import java.util.Optional;

/** Các tình huống QC thủ công Recurring (mục 1–14 trong biên bản API). */
public enum RecurringManualScenario {
    TOKEN_AUTH_SUCCESS("1", "Hệ thống merchant gửi yêu cầu tạo Token authentication thành công"),
    TOKEN_AUTH_FAILED("2", "Yêu cầu tạo Token authentication không hợp lệ"),
    REGISTER_SUCCESS("3", "Đăng ký thành công"),
    REGISTER_FAILED("4", "Đăng ký không thành công"),
    CARD_VERIFY_SUCCESS("5", "Khách hàng xác thực thẻ thành công"),
    CARD_VERIFY_FAILED("6", "Khách hàng xác thực thẻ không thành công"),
    RECURRING_PAY_SUCCESS("7", "Thanh toán định kỳ thành công"),
    RECURRING_PAY_FAILED("8", "Thanh toán định kỳ không thành công"),
    UPDATE_CARD_SUCCESS("9", "Gửi yêu cầu cập nhật thông tin thẻ thành công"),
    UPDATE_CARD_FAILED("10", "Gửi yêu cầu cập nhật thông tin thẻ không thành công"),
    UPDATE_PERIOD_SUCCESS("11", "Gửi yêu cầu cập nhật số kỳ thành công"),
    UPDATE_PERIOD_FAILED("12", "Gửi yêu cầu cập nhật số kỳ không thành công"),
    CANCEL_REGISTER_SUCCESS("13", "Gửi yêu cầu hủy đăng ký giao dịch định kỳ thành công"),
    CANCEL_REGISTER_FAILED("14", "Gửi yêu cầu hủy đăng ký giao dịch định kỳ không thành công");

    private final String caseNo;
    private final String situationPrefix;

    RecurringManualScenario(String caseNo, String situationPrefix) {
        this.caseNo = caseNo;
        this.situationPrefix = situationPrefix;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public String getSituationPrefix() {
        return situationPrefix;
    }

    public static Optional<RecurringManualScenario> byCaseNo(String caseNo) {
        if (caseNo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.caseNo.equals(caseNo.trim()))
                .findFirst();
    }

    public static Optional<RecurringManualScenario> bySituationPrefix(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim();
        return Arrays.stream(values())
                .filter(s -> normalized.startsWith(s.situationPrefix))
                .findFirst();
    }
}
