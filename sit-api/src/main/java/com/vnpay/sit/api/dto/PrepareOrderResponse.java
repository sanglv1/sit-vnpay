package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrepareOrderResponse {
    private final String txnRef;
    private final long amountVnd;
    private final String prepareUrl;
}
