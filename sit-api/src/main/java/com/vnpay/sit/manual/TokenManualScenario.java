package com.vnpay.sit.manual;

import java.util.Arrays;
import java.util.Optional;

/** Các tình huống QC thủ công Token (mục 1–8 trong biên bản). */
public enum TokenManualScenario {
    TOKEN_CREATE_SUCCESS("1", "Tạo Token thành công"),
    TOKEN_CREATE_FAILED("2", "Tạo token không thành công"),
    PAY_AND_CREATE_SUCCESS("3", "Thanh toán và tạo Token liên kết thẻ thành công"),
    PAY_AND_CREATE_FAILED("4", "Thanh toán và tạo Token liên kết thẻ không thành công"),
    TOKEN_PAY_SUCCESS("5", "Thanh toán bằng mã Token thành công"),
    TOKEN_PAY_FAILED("6", "Thanh toán bằng mã Token không thành công"),
    TOKEN_REMOVE_SUCCESS("7", "Xóa Token thành công"),
    TOKEN_REMOVE_FAILED("8", "Xóa Token không thành công");

    private final String caseNo;
    private final String situationPrefix;

    TokenManualScenario(String caseNo, String situationPrefix) {
        this.caseNo = caseNo;
        this.situationPrefix = situationPrefix;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public String getSituationPrefix() {
        return situationPrefix;
    }

    public static Optional<TokenManualScenario> byCaseNo(String caseNo) {
        if (caseNo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.caseNo.equals(caseNo.trim()))
                .findFirst();
    }

    public static Optional<TokenManualScenario> bySituationPrefix(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim();
        return Arrays.stream(values())
                .filter(s -> normalized.startsWith(s.situationPrefix))
                .findFirst();
    }
}
