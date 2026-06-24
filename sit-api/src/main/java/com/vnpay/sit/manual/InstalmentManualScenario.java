package com.vnpay.sit.manual;

import java.util.Arrays;
import java.util.Optional;

/** Các tình huống QC thủ công Instalment (mục 1–8 trong biên bản). */
public enum InstalmentManualScenario {
    TOKEN_AUTH_SUCCESS("1", "Hệ thống merchant gửi yêu cầu tạo Token authentication thành công"),
    TOKEN_AUTH_FAILED("2", "Yêu cầu tạo Token authentication không hợp lệ"),
    QUERY_CONFIG_SUCCESS("3", "Truy vấn thành công"),
    QUERY_CONFIG_FAILED("4", "Truy vấn thất bại"),
    CREATE_TXN_SUCCESS("5", "Khởi tạo giao dịch trả góp thành công"),
    CREATE_TXN_FAILED("6", "Khởi tạo giao dịch trả góp thất bại"),
    PAY_SUCCESS("7", "Thanh toán trả góp thành công"),
    PAY_FAILED("8", "Thanh toán trả góp không thành công");

    private final String caseNo;
    private final String situationPrefix;

    InstalmentManualScenario(String caseNo, String situationPrefix) {
        this.caseNo = caseNo;
        this.situationPrefix = situationPrefix;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public String getSituationPrefix() {
        return situationPrefix;
    }

    public static Optional<InstalmentManualScenario> byCaseNo(String caseNo) {
        if (caseNo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.caseNo.equals(caseNo.trim()))
                .findFirst();
    }

    public static Optional<InstalmentManualScenario> bySituationPrefix(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim();
        return Arrays.stream(values())
                .filter(s -> normalized.startsWith(s.situationPrefix))
                .findFirst();
    }
}
