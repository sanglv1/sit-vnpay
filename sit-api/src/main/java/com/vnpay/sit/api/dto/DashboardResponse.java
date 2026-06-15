package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponse {
    private final int terminalCount;
    private final int terminalDeltaMonth;
    private final long sessionsThisWeek;
    private final int sessionsWeekChangePercent;
    private final long testCasePassed;
    private final long testCaseFailed;
    private final int passRatePercent;
    private final String topFailHint;
    private final List<DashboardChartDay> chartLast7Days;
    private final List<DashboardRecentSession> recentSessions;
}
