package com.vnpay.sit.api.dto;

import com.vnpay.sit.model.PaymentFlow;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionWorkspaceResponse {
    private final TestSessionResponse session;
    private final PaymentFlow partnerFlow;
    private final List<TestRunResponse> latestRuns;
    private final List<EnumOption> testCases;
    private final List<EnumOption> recurringIpnCommands;
    private final List<EnumOption> tokenIpnCommands;
}
