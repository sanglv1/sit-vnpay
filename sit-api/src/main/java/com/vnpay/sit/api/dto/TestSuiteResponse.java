package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestSuiteResponse {
    private final Long sessionId;
    private final String txnRef;
    private final String failedTxnRef;
    private final String partnerName;
    private final int totalSteps;
    private final int passedSteps;
    private final boolean allPassed;
    private final List<TestSuiteStepResponse> steps;
}
