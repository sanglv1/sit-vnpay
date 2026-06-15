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
            return buildGlobal(null);
        }
        return buildGlobal(accessControlService.currentUserEmail(principal));
    }

    private DashboardResponse buildGlobal(String ownerEmail) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        int terminalCount = (int) partnerRepository.findByActiveTrueOrderByNameAsc().size();
        long terminalsThisMonth = partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqual(startOfMonth);
        long terminalsLastMonth = partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                startOfLastMonth, startOfMonth);

        long sessionsThisWeek = ownerEmail == null
                ? sessionRepository.countByCreatedAtGreaterThanEqual(startOfWeek)
                : sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
                        ownerEmail, startOfWeek);
        long sessionsLastWeek = ownerEmail == null
                ? sessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        startOfLastWeek, startOfWeek)
                : sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        ownerEmail, startOfLastWeek, startOfWeek);

        long testCasePassed = testRunRepository.countByPassedTrue();
        long testCaseFailed = testRunRepository.countByPassedFalse();
        int passRatePercent = totalCases(testCasePassed, testCaseFailed) == 0
                ? 0
                : (int) Math.round(testCasePassed * 100.0 / totalCases(testCasePassed, testCaseFailed));

        String topFailHint = buildTopFailHint();

        List<DashboardChartDay> chart = buildChart(today);
        List<DashboardRecentSession> recentSessions = buildRecentSessions(ownerEmail);

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

    private List<DashboardChartDay> buildChart(LocalDate today) {
        List<DashboardChartDay> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.atTime(LocalTime.MAX);
            long passed = testRunRepository.countByPassedTrueAndCreatedAtBetween(from, to);
            long failed = testRunRepository.countByPassedFalseAndCreatedAtBetween(from, to);
            days.add(DashboardChartDay.builder()
                    .label(day.format(CHART_LABEL))
                    .passed(passed)
                    .failed(failed)
                    .build());
        }
        return days;
    }

    private List<DashboardRecentSession> buildRecentSessions(String ownerEmail) {
        if (ownerEmail == null) {
            return sessionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                    .getContent()
                    .stream()
                    .map(this::toRecentSession)
                    .toList();
        }
        return sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
                        ownerEmail, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(this::toRecentSession)
                .toList();
    }

    private DashboardRecentSession toRecentSession(TestSession session) {
        List<TestRun> runs = testRunRepository.findBySessionIdOrderByCreatedAtDesc(session.getId());
        long autoPassed = runs.stream()
                .filter(r -> AUTO_CASES.contains(r.getTestCase()))
                .filter(TestRun::isPassed)
                .map(TestRun::getTestCase)
                .distinct()
                .count();
        int autoTotal = AUTO_CASES.size();
        boolean hasRuns = !runs.isEmpty();
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

    private String buildTopFailHint() {
        List<TestRun> failedRuns = testRunRepository.findByPassedFalseOrderByCreatedAtDesc(PageRequest.of(0, 200))
                .getContent();
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

    private static int percentChange(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round((current - previous) * 100.0 / previous);
    }
}
