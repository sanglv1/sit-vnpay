package com.vnpay.sit.testrun.dto;

import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.TokenIpnCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestRunForm {

    @NotNull(message = "Chọn đối tác")
    private Long partnerId;

    private Long sessionId;

    @NotNull(message = "Chọn loại callback")
    private CallbackType callbackType;

    @NotNull(message = "Chọn test case")
    private TestCaseType testCase;

    @NotBlank(message = "Mã giao dịch không được để trống")
    private String txnRef;

    @Min(value = 1, message = "Số tiền phải lớn hơn 0")
    private long amountVnd = 100_000;

    private Long wrongAmountVnd;

    /** Chỉ áp dụng khi partner flow = RECURRING; null → mặc định {@code recurring}. */
    private RecurringIpnCommand recurringIpnCommand;

    /** Chỉ áp dụng khi partner flow = TOKEN; null → mặc định {@code pay_and_create}. */
    private TokenIpnCommand tokenIpnCommand;
}
