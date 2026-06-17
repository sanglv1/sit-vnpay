package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionWorkspaceResponse {
    private final TestSessionResponse session;
    private final List<TestRunResponse> latestRuns;
    private final List<EnumOption> testCases;
}
