package com.vnpay.sit.testrun.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrepareMerchantOrderForm {
    @NotNull
    private Long partnerId;

    @NotNull
    @Min(1)
    private Long amountVnd;
}
