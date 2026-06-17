package com.vnpay.sit.dashboard;

import com.vnpay.sit.api.dto.DashboardChartDay;
import com.vnpay.sit.api.dto.DashboardRecentSession;
import com.vnpay.sit.api.dto.DashboardResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Set<TestCaseType> AUTO_CASES = EnumSet.copyOf(TestCaseType.ipnSuiteExecutionOrder());
    private static final DateTimeFormatter CHART_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final PartnerConfigRepository partnerRepository;
    private final TestSessionRepository sessionRepository;
    private final TestRunRepository testRunRepository;
    private final AccessControlService accessControlService;

    public DashboardService(
            PartnerConfigRepository partnerRepository,
            TestSessionRepository sessionRepository,
            TestRunRepository testRunRepository,
            AccessControlService accessControlService
    ) {
        this.partnerRepository = partnerRepository;
        this.sessionRepository = sessionRepository;
        this.testRunRepository = testRunRepository;
        this.accessControlService = accessControlService;
    }

    public DashboardResponse build(SitUserPrincipal principal) {
        if (accessControlService.isAdmin(principal)) {
            return buildGlobal(null, null);
        }
        String creatorEmail = accessControlService.currentUserEmail(principal);
        List<Long> sessionIds = sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(creatorEmail)
                .stream()
                .map(TestSession::getId)
                .toList();
        return buildGlobal(creatorEmail, sessionIds);
    }

    private DashboardResponse buildGlobal(String creatorEmail, List<Long> sessionIds) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        int terminalCount = creatorEmail == null
                ? (int) partnerRepository.findByActiveTrueOrderByNameAsc().size()
                : (int) partnerRepository.countByActiveTrueAndCreatedByEmailIgnoreCase(creatorEmail);
        long terminalsThisMonth = creatorEmail == null
                ? partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqual(startOfMonth)
                : partnerRepository.countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
                        creatorEmail, startOfMonth);
        long terminalsLastMonth = creatorEmail == null
                ? partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        startOfLastMonth, startOfMonth)
                : partnerRepository.countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        creatorEmail, startOfLastMonth, startOfMonth);

        long sessionsThisWeek = creatorEmail == null
                ? sessionRepository.countByCreatedAtGreaterThanEqual(startOfWeek)
                : sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
                        creatorEmail, startOfWeek);
        long sessionsLastWeek = creatorEmail == null
                ? sessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        startOfLastWeek, startOfWeek)
                : sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        creatorEmail, startOfLastWeek, startOfWeek);

        long testCasePassed = sessionIds == null
                ? testRunRepository.countByPassedTrue()
                : countPassedInSessions(sessionIds);
        long testCaseFailed = sessionIds == null
                ? testRunRepository.countByPassedFalse()
                : countFailedInSessions(sessionIds);
        int passRatePercent = totalCases(testCasePassed, testCaseFailed) == 0
                ? 0
                : (int) Math.round(testCasePassed * 100.0 / totalCases(testCasePassed, testCaseFailed));

        String topFailHint = buildTopFailHint(sessionIds);

        List<DashboardChartDay> chart = buildChart(today, sessionIds);
        List<DashboardRecentSession> recentSessions = buildRecentSessions(creatorEmail);

        return DashboardResponse.builder()
                .terminalCount(terminalCount)
                .terminalDeltaMonth((int) Math.max(0, terminalsThisMonth - terminalsLastMonth))
                .sessionsThisWeek(sessionsThisWeek)
                .sessionsWeekChangePercent(percentChange(sessionsLastWeek, sessionsThisWeek))
                .testCasePassed(testCasePassed)
                .testCaseFailed(testCaseFailed)
                .passRatePercent(passRatePercent)
                .topFailHint(topFailHint)
                .chartLast7Days(chart)
                .recentSessions(recentSessions)
                .build();
    }

    private List<DashboardChartDay> buildChart(LocalDate today, List<Long> sessionIds) {
        List<DashboardChartDay> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.atTime(LocalTime.MAX);
            long passed = sessionIds == null
                    ? testRunRepository.countByPassedTrueAndCreatedAtBetween(from, to)
                    : countPassedInSessionsBetween(sessionIds, from, to);
            long failed = sessionIds == null
                    ? testRunRepository.countByPassedFalseAndCreatedAtBetween(from, to)
                    : countFailedInSessionsBetween(sessionIds, from, to);
            days.add(DashboardChartDay.builder()
                    .label(day.format(CHART_LABEL))
                    .passed(passed)
                    .failed(failed)
                    .build());
        }
        return days;
    }

    private List<DashboardRecentSession> buildRecentSessions(String ownerEmail) {
        List<TestSession> sessions = ownerEmail == null
                ? sessionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getContent()
                : sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
                        ownerEmail, PageRequest.of(0, 5)).getContent();
        Map<Long, Integer> passedAutoBySession = countPassedAutoCasesForSessions(sessions);
        Map<Long, Long> runCountBySession = countRunsBySession(sessions);
        return sessions.stream()
                .map(session -> toRecentSession(session, passedAutoBySession, runCountBySession))
                .toList();
    }

    private DashboardRecentSession toRecentSession(
            TestSession session,
            Map<Long, Integer> passedAutoBySession,
            Map<Long, Long> runCountBySession
    ) {
        int autoPassed = passedAutoBySession.getOrDefault(session.getId(), 0);
        int autoTotal = AUTO_CASES.size();
        boolean hasRuns = runCountBySession.getOrDefault(session.getId(), 0L) > 0;
        boolean passed = hasRuns && autoPassed == autoTotal;
        String statusLabel = !hasRuns ? "OPEN" : (passed ? "PASSED" : "FAILED");

        return DashboardRecentSession.builder()
                .id(session.getId())
                .tmnCode(session.getTmnCode())
                .statusLabel(statusLabel)
                .passed(passed)
                .createdByEmail(session.getCreatedByEmail() != null ? session.getCreatedByEmail() : "—")
                .createdAt(session.getCreatedAt())
                .build();
    }

    private Map<Long, Integer> countPassedAutoCasesForSessions(List<TestSession> sessions) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = sessions.stream().map(TestSession::getId).toList();
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : testRunRepository.countDistinctPassedAutoCasesBySessionIds(sessionIds, AUTO_CASES)) {
            Long sessionId = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            result.put(sessionId, count);
        }
        return result;
    }

    private Map<Long, Long> countRunsBySession(List<TestSession> sessions) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = sessions.stream().map(TestSession::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : testRunRepository.countRunsBySessionIds(sessionIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private String buildTopFailHint(List<Long> sessionIds) {
        List<TestRun> failedRuns = sessionIds == null
                ? testRunRepository.findByPassedFalseOrderByCreatedAtDesc(PageRequest.of(0, 200)).getContent()
                : (sessionIds.isEmpty()
                        ? List.of()
                        : testRunRepository.findByPassedFalseAndSessionIdInOrderByCreatedAtDesc(
                                sessionIds, PageRequest.of(0, 200)).getContent());
        if (failedRuns.isEmpty()) {
            return "Chưa có case Fail";
        }
        Map<TestCaseType, Long> byCase = failedRuns.stream()
                .collect(Collectors.groupingBy(TestRun::getTestCase, Collectors.counting()));
        TestCaseType top = byCase.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(TestCaseType.INVALID_HASH);

        if (top == TestCaseType.INVALID_HASH) {
            return "Chủ yếu do sai chữ ký (97)";
        }
        return "Chủ yếu: " + top.getLabel();
    }

    private static long totalCases(long passed, long failed) {
        return passed + failed;
    }

    private long countPassedInSessions(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return testRunRepository.countByPassedTrueAndSessionIdIn(sessionIds);
    }

    private long countFailedInSessions(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return testRunRepository.countByPassedFalseAndSessionIdIn(sessionIds);
    }

    private long countPassedInSessionsBetween(List<Long> sessionIds, LocalDateTime from, LocalDateTime to) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return testRunRepository.countByPassedTrueAndSessionIdInAndCreatedAtBetween(sessionIds, from, to);
    }

    private long countFailedInSessionsBetween(List<Long> sessionIds, LocalDateTime from, LocalDateTime to) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        return testRunRepository.countByPassedFalseAndSessionIdInAndCreatedAtBetween(sessionIds, from, to);
    }

    private static int percentChange(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round((current - previous) * 100.0 / previous);
    }
}
