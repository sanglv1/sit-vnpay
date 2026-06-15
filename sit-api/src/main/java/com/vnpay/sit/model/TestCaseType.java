package com.vnpay.sit.model;

import java.util.List;

public enum TestCaseType {
    UNKNOWN_ERROR("Lỗi không xác định (exception)", "99", "ex", 0),
    INVALID_HASH("Chữ ký không hợp lệ (Case 1)", "97", "1", 1),
    ORDER_NOT_FOUND("Không tìm thấy đơn hàng (Case 2)", "01", "2", 2),
    WRONG_AMOUNT("Sai số tiền (Case 3)", "04", "3", 3),
    ORDER_ALREADY_CONFIRMED("Đơn đã xác nhận (Case 4)", "02", "4", 4),
    SUCCESS("Giao dịch thành công (Case 5)", "00", "5", 5),
    FAILED("Giao dịch thất bại (Case 6)", "00", "6", 6);

    private final String label;
    private final String expectedRspCode;
    private final String caseCode;
    private final int checkOrder;

    TestCaseType(String label, String expectedRspCode, String caseCode, int checkOrder) {
        this.label = label;
        this.expectedRspCode = expectedRspCode;
        this.caseCode = caseCode;
        this.checkOrder = checkOrder;
    }

    public String getLabel() {
        return label;
    }

    public String getExpectedRspCode() {
        return expectedRspCode;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public int getCheckOrder() {
        return checkOrder;
    }

    /** Thứ tự thực thi suite IPN tự động (không gồm case ex — nghiệm thu thủ công). */
    public static List<TestCaseType> ipnSuiteExecutionOrder() {
        return List.of(
                INVALID_HASH,
                ORDER_NOT_FOUND,
                WRONG_AMOUNT,
                FAILED,
                SUCCESS,
                ORDER_ALREADY_CONFIRMED
        );
    }

    public static List<TestCaseType> autoIpnTestCases() {
        return ipnSuiteExecutionOrder();
    }

    public boolean isManualOnly() {
        return this == UNKNOWN_ERROR;
    }
}
