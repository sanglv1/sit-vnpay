package com.vnpay.sit.manual;

import java.util.Arrays;
import java.util.Optional;

/** Các tình huống QC thủ công PreAuth / pa-svc (mục 1–17 trong biên bản). */
public enum PreAuthManualScenario {
    TOKEN_AUTH_SUCCESS("1", "Hệ thống merchant gửi yêu cầu tạo Token authentication thành công"),
    TOKEN_AUTH_FAILED("2", "Yêu cầu tạo Token authentication không hợp lệ"),
    CARD_LINK_INIT_SUCCESS("3", "Khách hàng thực hiện tạo liên kết thẻ"),
    CARD_LINK_INIT_FAILED("4", "Yêu cầu liên kết thẻ thanh toán không hợp l"),
    CARD_LINK_VERIFY_SUCCESS("5", "Khách hàng thực hiện liên kết thẻ thanh toán thành công"),
    CARD_LINK_VERIFY_FAILED("6", "Khách hàng thực hiện liên kết thẻ thanh toán không thành công"),
    PREAUTH_INIT_SUCCESS("7", "Hệ thống merchant gửi yêu cầu khởi tạo giao dịch ủy quyền thành công"),
    PREAUTH_INIT_FAILED("8", "Hệ thống merchant gửi yêu cầu khởi tạo giao dịch ủy quyền không thành công"),
    PREAUTH_3DS_SUCCESS("9", "Khách hàng thực hiện nhập thông tin xác thực giao dịch ủy quyền thành công"),
    PREAUTH_3DS_FAILED("10", "Khách hàng thực hiện nhập thông tin xác thực giao dịch ủy quyền không thành công"),
    CAPTURE_SUCCESS("11", "Capture giao dịch thành công"),
    CAPTURE_FAILED("12", "Capture giao dịch thất bại"),
    CAPTURE_DUPLICATE("13", "Capture giao dịch trùng lặp"),
    REVERSAL_SUCCESS("14", "Hủy giao dịch ủy quyền thành công"),
    REVERSAL_FAILED("15", "Hủy giao dịch ủy quyền thất bại"),
    REMOVE_TOKEN_SUCCESS("16", "Xóa liên kết thẻ thành công"),
    REMOVE_TOKEN_FAILED("17", "Xóa liên kết thẻ thất bại");

    private final String caseNo;
    private final String situationPrefix;

    PreAuthManualScenario(String caseNo, String situationPrefix) {
        this.caseNo = caseNo;
        this.situationPrefix = situationPrefix;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public String getSituationPrefix() {
        return situationPrefix;
    }

    public static Optional<PreAuthManualScenario> byCaseNo(String caseNo) {
        if (caseNo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.caseNo.equals(caseNo.trim()))
                .findFirst();
    }

    public static Optional<PreAuthManualScenario> bySituationPrefix(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim();
        return Arrays.stream(values())
                .filter(s -> normalized.startsWith(s.situationPrefix))
                .findFirst();
    }
}
