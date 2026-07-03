package com.vnpay.sit.manual;

import java.util.Arrays;
import java.util.Optional;

/** Các tình huống QC thủ công QR Direct / Merchant Hosted QR (mục 1–10 trong biên bản). */
public enum QrDirectManualScenario {
    CREATE_PAY_SUCCESS("1", "Tạo yêu cầu thanh toán thành công"),
    CREATE_PAY_FAILED("2", "Tạo yêu cầu thanh toán không thành công"),
    QR_SCAN_PAY_SUCCESS("3", "Thanh toán thành công"),
    QR_SCAN_PAY_FAILED("4", "Thanh toán không thành công"),
    DEEPLINK_CREATE_SUCCESS("5", "Deep-link: tạo yêu cầu thanh toán thành công"),
    DEEPLINK_CREATE_FAILED("6", "Deep-link: tạo yêu cầu thanh toán không thành công"),
    SUPPORT_APPS_SUCCESS("7", "Lấy danh sách ứng dụng hỗ trợ VNPAY-QR thành công"),
    OPEN_APP_SUCCESS("8", "Gọi mở ứng dụng thành công"),
    OPEN_APP_NOT_INSTALLED("9", "Không mở được ứng dụng thanh toán"),
    OPEN_APP_FAILED("10", "Gọi mở ứng dụng thất bại");

    private final String caseNo;
    private final String situationPrefix;

    QrDirectManualScenario(String caseNo, String situationPrefix) {
        this.caseNo = caseNo;
        this.situationPrefix = situationPrefix;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public String getSituationPrefix() {
        return situationPrefix;
    }

    public static Optional<QrDirectManualScenario> byCaseNo(String caseNo) {
        if (caseNo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.caseNo.equals(caseNo.trim()))
                .findFirst();
    }

    public static Optional<QrDirectManualScenario> bySituationPrefix(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim();
        return Arrays.stream(values())
                .filter(s -> normalized.startsWith(s.situationPrefix))
                .findFirst();
    }
}
