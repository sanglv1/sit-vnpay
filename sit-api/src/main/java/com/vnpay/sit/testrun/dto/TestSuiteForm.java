package com.vnpay.sit.testrun.dto;

import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TokenIpnCommand;
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

    /** Đơn Case 5 — IPN giao dịch thành công (và Case 4 sau khi đã SUCCESS). */
    @NotBlank(message = "Mã giao dịch thành công (Case 5) không được để trống")
    private String txnRef;

    @Min(value = 1, message = "Số tiền phải lớn hơn 0")
    private long amountVnd = 100_000;

    /** Đơn Case 6 — IPN giao dịch thất bại (txnRef khác Case 5). */
    @NotBlank(message = "Mã giao dịch thất bại (Case 6) không được để trống")
    private String failedTxnRef;

    private Long failedAmountVnd;

    /** Số tiền sai cho Case 3; mặc định = amountVnd + 1000 nếu không nhập. */
    private Long wrongAmountVnd;

    /** Chỉ áp dụng khi partner flow = RECURRING; null → mặc định {@code recurring}. */
    private RecurringIpnCommand recurringIpnCommand;

    /** Chỉ áp dụng khi partner flow = TOKEN; null → mặc định {@code pay_and_create}. */
    private TokenIpnCommand tokenIpnCommand;
}
