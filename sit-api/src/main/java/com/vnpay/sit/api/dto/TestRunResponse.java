package com.vnpay.sit.api.dto;

import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestRunResponse {
    private final Long id;
    private final Long partnerId;
    private final String partnerName;
    private final PaymentFlow flow;
    private final CallbackType callbackType;
    private final TestCaseType testCase;
    private final String testCaseLabel;
    private final String txnRef;
    private final String targetUrl;
    private final String requestParams;
    private final Integer httpStatus;
    private final String responseBody;
    private final String expectedRspCode;
    private final String actualRspCode;
    private final boolean passed;
    private final Long durationMs;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    public static TestRunResponse from(TestRun run) {
        return TestRunResponse.builder()
                .id(run.getId())
                .partnerId(run.getPartnerId())
                .partnerName(run.getPartnerName())
                .flow(run.getFlow())
                .callbackType(run.getCallbackType())
                .testCase(run.getTestCase())
                .testCaseLabel(run.getTestCase().getLabel())
                .txnRef(run.getTxnRef())
                .targetUrl(run.getTargetUrl())
                .requestParams(run.getRequestParams())
                .httpStatus(run.getHttpStatus())
                .responseBody(run.getResponseBody())
                .expectedRspCode(run.getExpectedRspCode())
                .actualRspCode(run.getActualRspCode())
                .passed(run.isPassed())
                .durationMs(run.getDurationMs())
                .errorMessage(run.getErrorMessage())
                .createdAt(run.getCreatedAt())
                .build();
    }
}
