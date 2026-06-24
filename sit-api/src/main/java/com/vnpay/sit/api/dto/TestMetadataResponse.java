package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestMetadataResponse {
    private final List<PartnerResponse> partners;
    private final List<EnumOption> callbackTypes;
    private final List<EnumOption> testCases;
    private final List<EnumOption> paymentFlows;
    private final List<EnumOption> recurringIpnCommands;
    private final List<EnumOption> tokenIpnCommands;
    private final String defaultTxnRef;
}
