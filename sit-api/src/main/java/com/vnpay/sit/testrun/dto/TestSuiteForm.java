package com.vnpay.sit.testrun.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestSuiteForm {

    @NotNull(message = "Chọn đối tác")
    private Long partnerId;

    private Long sessionId;

    @NotBlank(message = "Mã giao dịch không được để trống")
    private String txnRef;

    @Min(value = 1, message = "Số tiền phải lớn hơn 0")
    private long amountVnd = 100_000;

    /** Số tiền sai cho Case 3; mặc định = amountVnd + 1000 nếu không nhập. */
    private Long wrongAmountVnd;
}
