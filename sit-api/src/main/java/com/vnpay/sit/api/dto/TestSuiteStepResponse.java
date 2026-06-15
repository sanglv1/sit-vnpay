package com.vnpay.sit.api.dto;

import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestSuiteStepResponse {
    private final int step;
    private final String caseCode;
    private final int checkOrder;
    private final TestCaseType testCase;
    private final String testCaseLabel;
    private final String expectedRspCode;
    private final String actualRspCode;
    private final boolean passed;
    private final Long testRunId;
    private final Integer httpStatus;
    private final String message;

    public static TestSuiteStepResponse from(int step, TestCaseType testCase, TestRun run) {
        return TestSuiteStepResponse.builder()
                .step(step)
                .caseCode(testCase.getCaseCode())
                .checkOrder(testCase.getCheckOrder())
                .testCase(testCase)
                .testCaseLabel(testCase.getLabel())
                .expectedRspCode(run.getExpectedRspCode())
                .actualRspCode(run.getActualRspCode())
                .passed(run.isPassed())
                .testRunId(run.getId())
                .httpStatus(run.getHttpStatus())
                .message(run.getErrorMessage())
                .build();
    }
}
