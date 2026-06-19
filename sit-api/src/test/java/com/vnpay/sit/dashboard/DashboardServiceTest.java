package com.vnpay.sit.dashboard;

import com.vnpay.sit.api.dto.DashboardResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock private PartnerConfigRepository partnerRepository;
  @Mock private TestSessionRepository sessionRepository;
  @Mock private TestRunRepository testRunRepository;
  @Mock private AccessControlService accessControlService;

  private DashboardService dashboardService;

  @BeforeEach
  void setUp() {
    dashboardService =
        new DashboardService(
            partnerRepository, sessionRepository, testRunRepository, accessControlService);
  }

  @Test
  void build_adminShouldAggregateGlobalStats() {
    PartnerConfig partner = new PartnerConfig();
    partner.setActive(true);
    when(accessControlService.isAdmin(any())).thenReturn(true);
    when(partnerRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(partner, partner));
    when(partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqual(any())).thenReturn(2L);
    when(partnerRepository.countByActiveTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            any(), any()))
        .thenReturn(1L);
    when(sessionRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(5L);
    when(sessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
        .thenReturn(2L);
    when(testRunRepository.countByPassedTrue()).thenReturn(8L);
    when(testRunRepository.countByPassedFalse()).thenReturn(2L);
    when(testRunRepository.findByPassedFalseOrderByCreatedAtDesc(any()))
        .thenReturn(new PageImpl<>(List.of(failedRun(TestCaseType.INVALID_HASH))));
    when(testRunRepository.countByPassedTrueAndCreatedAtBetween(any(), any())).thenReturn(1L);
    when(testRunRepository.countByPassedFalseAndCreatedAtBetween(any(), any())).thenReturn(0L);
    when(sessionRepository.findAllByOrderByCreatedAtDesc(any()))
        .thenReturn(
            new PageImpl<>(
                List.of(session(1L, "TMN001", "qc@merchant.com", LocalDateTime.now())),
                PageRequest.of(0, 5),
                1));
    when(testRunRepository.countDistinctPassedAutoCasesBySessionIds(
            eq(List.of(1L)), eq(EnumSet.copyOf(TestCaseType.ipnSuiteExecutionOrder()))))
        .thenReturn(List.<Object[]>of(new Object[] {1L, 6}));
    when(testRunRepository.countRunsBySessionIds(List.of(1L)))
        .thenReturn(List.<Object[]>of(new Object[] {1L, 6L}));

    DashboardResponse response =
        dashboardService.build(principal("admin@vnpay.vn", UserRole.ADMIN));

    assertThat(response.getTerminalCount()).isEqualTo(2);
    assertThat(response.getTerminalDeltaMonth()).isEqualTo(1);
    assertThat(response.getSessionsThisWeek()).isEqualTo(5);
    assertThat(response.getSessionsWeekChangePercent()).isEqualTo(150);
    assertThat(response.getTestCasePassed()).isEqualTo(8);
    assertThat(response.getTestCaseFailed()).isEqualTo(2);
    assertThat(response.getPassRatePercent()).isEqualTo(80);
    assertThat(response.getTopFailHint()).isEqualTo("Chủ yếu do sai chữ ký (97)");
    assertThat(response.getChartLast7Days()).hasSize(7);
    assertThat(response.getRecentSessions()).hasSize(1);
    assertThat(response.getRecentSessions().get(0).getTmnCode()).isEqualTo("TMN001");
    assertThat(response.getRecentSessions().get(0).isPassed()).isTrue();
  }

  @Test
  void build_merchantShouldScopeStatsToOwnSessions() {
    TestSession session = session(10L, "TMNQC", "qc@merchant.com", LocalDateTime.now());
    when(accessControlService.isAdmin(any())).thenReturn(false);
    when(accessControlService.currentUserEmail(any())).thenReturn("qc@merchant.com");
    when(sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc("qc@merchant.com"))
        .thenReturn(List.of(session));
    when(partnerRepository.countByActiveTrueAndCreatedByEmailIgnoreCase("qc@merchant.com"))
        .thenReturn(1L);
    when(partnerRepository
            .countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
                eq("qc@merchant.com"), any()))
        .thenReturn(1L);
    when(partnerRepository
            .countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq("qc@merchant.com"), any(), any()))
        .thenReturn(0L);
    when(sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
            eq("qc@merchant.com"), any()))
        .thenReturn(1L);
    when(sessionRepository
            .countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq("qc@merchant.com"), any(), any()))
        .thenReturn(0L);
    when(testRunRepository.countByPassedTrueAndSessionIdIn(List.of(10L))).thenReturn(3L);
    when(testRunRepository.countByPassedFalseAndSessionIdIn(List.of(10L))).thenReturn(1L);
    when(testRunRepository.findByPassedFalseAndSessionIdInOrderByCreatedAtDesc(
            eq(List.of(10L)), any()))
        .thenReturn(new PageImpl<>(List.of(failedRun(TestCaseType.WRONG_AMOUNT))));
    when(testRunRepository.countByPassedTrueAndSessionIdInAndCreatedAtBetween(
            eq(List.of(10L)), any(), any()))
        .thenReturn(1L);
    when(testRunRepository.countByPassedFalseAndSessionIdInAndCreatedAtBetween(
            eq(List.of(10L)), any(), any()))
        .thenReturn(0L);
    when(sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
            eq("qc@merchant.com"), any()))
        .thenReturn(
            new PageImpl<>(List.of(session), PageRequest.of(0, 5), 1));
    when(testRunRepository.countDistinctPassedAutoCasesBySessionIds(
            eq(List.of(10L)), eq(EnumSet.copyOf(TestCaseType.ipnSuiteExecutionOrder()))))
        .thenReturn(List.<Object[]>of(new Object[] {10L, 2}));
    when(testRunRepository.countRunsBySessionIds(List.of(10L)))
        .thenReturn(List.<Object[]>of(new Object[] {10L, 3L}));

    DashboardResponse response =
        dashboardService.build(principal("qc@merchant.com", UserRole.MERCHANT_QC));

    assertThat(response.getTerminalCount()).isEqualTo(1);
    assertThat(response.getTestCasePassed()).isEqualTo(3);
    assertThat(response.getTestCaseFailed()).isEqualTo(1);
    assertThat(response.getPassRatePercent()).isEqualTo(75);
    assertThat(response.getTopFailHint()).contains("Sai số tiền");
    assertThat(response.getRecentSessions().get(0).getStatusLabel()).isEqualTo("FAILED");
  }

  @Test
  void build_merchantWithNoSessionsShouldReturnEmptyHints() {
    when(accessControlService.isAdmin(any())).thenReturn(false);
    when(accessControlService.currentUserEmail(any())).thenReturn("new@merchant.com");
    when(sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc("new@merchant.com"))
        .thenReturn(List.of());
    when(partnerRepository.countByActiveTrueAndCreatedByEmailIgnoreCase("new@merchant.com"))
        .thenReturn(0L);
    when(partnerRepository
            .countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
                eq("new@merchant.com"), any()))
        .thenReturn(0L);
    when(partnerRepository
            .countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq("new@merchant.com"), any(), any()))
        .thenReturn(0L);
    when(sessionRepository.countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
            eq("new@merchant.com"), any()))
        .thenReturn(0L);
    when(sessionRepository
            .countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq("new@merchant.com"), any(), any()))
        .thenReturn(0L);
    when(sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
            eq("new@merchant.com"), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

    DashboardResponse response =
        dashboardService.build(principal("new@merchant.com", UserRole.MERCHANT_QC));

    assertThat(response.getTestCasePassed()).isZero();
    assertThat(response.getTestCaseFailed()).isZero();
    assertThat(response.getPassRatePercent()).isZero();
    assertThat(response.getTopFailHint()).isEqualTo("Chưa có case Fail");
    assertThat(response.getRecentSessions()).isEmpty();
  }

  private static TestRun failedRun(TestCaseType testCase) {
    TestRun run = new TestRun();
    run.setTestCase(testCase);
    run.setPassed(false);
    return run;
  }

  private static TestSession session(
      Long id, String tmnCode, String createdByEmail, LocalDateTime createdAt) {
    TestSession session = new TestSession();
    session.setId(id);
    session.setTmnCode(tmnCode);
    session.setCreatedByEmail(createdByEmail);
    session.setCreatedAt(createdAt);
    return session;
  }

  private static SitUserPrincipal principal(String email, UserRole role) {
    SitUser user = new SitUser();
    user.setId(1L);
    user.setEmail(email);
    user.setRole(role);
    user.setActive(true);
    return new SitUserPrincipal(user);
  }
}
