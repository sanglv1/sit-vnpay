package com.vnpay.sit.testrun.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.api.dto.TestSuiteResponse;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.user.entity.SitUser;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.session.service.TestSessionService;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.runner.CallbackHttpRunner;
import com.vnpay.sit.testrun.dto.TestRunForm;
import com.vnpay.sit.testrun.dto.TestSuiteForm;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestExecutionServiceTest {

    private static final String SECRET = "TEST_SECRET_KEY_12345";

    @Mock
    private PartnerService partnerService;

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private CallbackHttpRunner httpRunner;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestSessionService testSessionService;

    private TestExecutionService testExecutionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PartnerConfig partner;

    @BeforeEach
    void setUp() {
        testExecutionService = new TestExecutionService(
                partnerService,
                testRunRepository,
                httpRunner,
                objectMapper,
                accessControlService,
                sessionRepository,
                testSessionService
        );

        partner = new PartnerConfig();
        partner.setId(1L);
        partner.setName("Demo Merchant");
        partner.setFlow(PaymentFlow.PAY);
        partner.setTmnCode("DEMO01");
        partner.setSecretKey(SECRET);
        partner.setIpnUrl("http://merchant.test/ipn");
        partner.setReturnUrl("http://merchant.test/return");
    }

    @Test
    void execute_ipnSuccess_shouldPassWhenRspCodeMatches() {
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);
        when(httpRunner.execute(eq(partner.getIpnUrl()), any(), eq(true)))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"00\"}"));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(invocation -> {
            TestRun run = invocation.getArgument(0);
            run.setId(10L);
            return run;
        });

        TestRunForm form = ipnForm(TestCaseType.SUCCESS);

        TestRun result = testExecutionService.execute(form, adminPrincipal());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getExpectedRspCode()).isEqualTo("00");
        assertThat(result.getActualRspCode()).isEqualTo("00");
        assertThat(result.getCallbackType()).isEqualTo(CallbackType.IPN);
        assertThat(result.getRequestParams()).contains("vnp_SecureHash");
    }

    @Test
    void execute_invalidHash_shouldPassWhenMerchantReturns97() {
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);
        when(httpRunner.execute(eq(partner.getIpnUrl()), any(), eq(true)))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"97\"}"));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestRun result = testExecutionService.execute(ipnForm(TestCaseType.INVALID_HASH), adminPrincipal());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getActualRspCode()).isEqualTo("97");
    }

    @Test
    void execute_invalidHash_shouldAttachInvalidHashField() {
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);
        when(httpRunner.execute(eq(partner.getIpnUrl()), any(), eq(true)))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"97\"}"));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        testExecutionService.execute(ipnForm(TestCaseType.INVALID_HASH), adminPrincipal());

        verify(httpRunner).execute(eq(partner.getIpnUrl()), paramsCaptor.capture(), eq(true));
        assertThat(paramsCaptor.getValue()).containsEntry("vnp_SecureHash", "invalid_sit_hash");
    }

    @Test
    void execute_returnUrl_shouldPassOn2xxResponse() {
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);
        when(httpRunner.execute(eq(partner.getReturnUrl()), any(), eq(false)))
                .thenReturn(callbackResponse(200, "OK"));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestRunForm form = ipnForm(TestCaseType.SUCCESS);
        form.setCallbackType(CallbackType.RETURN);

        TestRun result = testExecutionService.execute(form, adminPrincipal());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getExpectedRspCode()).isNull();
    }

    @Test
    void execute_missingIpnUrl_shouldThrow() {
        partner.setIpnUrl("");
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);

        assertThatThrownBy(() -> testExecutionService.execute(ipnForm(TestCaseType.SUCCESS), adminPrincipal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPN URL");
    }

    @Test
    void execute_unknownPartner_shouldThrow() {
        when(partnerService.requireAccessible(eq(99L), any()))
                .thenThrow(new IllegalArgumentException("Không tìm thấy đối tác"));

        TestRunForm form = ipnForm(TestCaseType.SUCCESS);
        form.setPartnerId(99L);

        assertThatThrownBy(() -> testExecutionService.execute(form, adminPrincipal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy đối tác");
    }

    @Test
    void executeIpnSuite_shouldRunSixCasesAndAggregateResults() {
        when(partnerService.requireAccessible(eq(1L), any())).thenReturn(partner);
        when(httpRunner.execute(eq(partner.getIpnUrl()), any(), eq(true)))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"00\"}"))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"01\"}"))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"04\"}"))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"00\"}"))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"00\"}"))
                .thenReturn(callbackResponse(200, "{\"RspCode\":\"02\"}"));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestSuiteForm form = new TestSuiteForm();
        form.setPartnerId(1L);
        form.setSessionId(5L);
        form.setTxnRef("SUITE001");
        form.setFailedTxnRef("SUITE002");
        form.setAmountVnd(150_000L);
        form.setWrongAmountVnd(151_000L);

        TestSuiteResponse suite = testExecutionService.executeIpnSuite(form, adminPrincipal());

        assertThat(suite.getTotalSteps()).isEqualTo(6);
        assertThat(suite.getTxnRef()).isEqualTo("SUITE001");
        assertThat(suite.getPartnerName()).isEqualTo("Demo Merchant");
        assertThat(suite.getSessionId()).isEqualTo(5L);
        verify(httpRunner, times(6)).execute(eq(partner.getIpnUrl()), any(), eq(true));
    }

    @Test
    void executeIpnSuite_sameTxnRefForSuccessAndFailed_shouldThrow() {
        TestSuiteForm form = new TestSuiteForm();
        form.setPartnerId(1L);
        form.setSessionId(5L);
        form.setTxnRef("SAME001");
        form.setFailedTxnRef("SAME001");
        form.setAmountVnd(100_000L);

        assertThatThrownBy(() -> testExecutionService.executeIpnSuite(form, adminPrincipal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải khác nhau");
    }

    @Test
    void getIpnSuiteResult_shouldBuildFromEffectiveSessionRuns() {
        when(testRunRepository.findBySessionIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(
                runForCase(TestCaseType.SUCCESS, false, "02", 210L),
                runForCase(TestCaseType.SUCCESS, true, "00", 200L),
                runForCase(TestCaseType.INVALID_HASH, true, "97", 201L)
        ));

        Optional<TestSuiteResponse> suite = testExecutionService.getIpnSuiteResult(2L);

        assertThat(suite).isPresent();
        assertThat(suite.get().getSessionId()).isEqualTo(2L);
        assertThat(suite.get().getPartnerName()).isEqualTo("Demo Merchant");
        assertThat(suite.get().getTotalSteps()).isEqualTo(6);
        assertThat(suite.get().getPassedSteps()).isEqualTo(2);
        assertThat(suite.get().isAllPassed()).isFalse();
        assertThat(suite.get().getSteps()).hasSize(6);
        assertThat(suite.get().getSteps().get(0).getTestCase()).isEqualTo(TestCaseType.INVALID_HASH);
        assertThat(suite.get().getSteps().get(0).isPassed()).isTrue();
        assertThat(suite.get().getSteps().stream()
                .filter(step -> step.getTestCase() == TestCaseType.SUCCESS)
                .findFirst()
                .orElseThrow()
                .isPassed()).isTrue();
        assertThat(suite.get().getSteps().get(5).getTestCase()).isEqualTo(TestCaseType.ORDER_ALREADY_CONFIRMED);
        assertThat(suite.get().getSteps().get(5).isPassed()).isFalse();
    }

    @Test
    void getIpnSuiteResult_withoutRuns_shouldReturnEmpty() {
        when(testRunRepository.findBySessionIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());

        assertThat(testExecutionService.getIpnSuiteResult(99L)).isEmpty();
    }

    @Test
    void findHistory_adminWithoutFilter_shouldReturnGlobalHistory() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<TestRun> expected = new PageImpl<>(List.of(new TestRun()), pageable, 1);
        when(accessControlService.isAdmin(any())).thenReturn(true);
        when(testRunRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(expected);

        assertThat(testExecutionService.findHistory(null, null, pageable, adminPrincipal()))
                .isSameAs(expected);
        verify(testRunRepository).findAllByOrderByCreatedAtDesc(pageable);
        verify(sessionRepository, never()).findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(any());
    }

    @Test
    void findHistory_merchantShouldOnlyUseOwnedSessions() {
        PageRequest pageable = PageRequest.of(0, 20);
        TestSession ownedSession = new TestSession();
        ownedSession.setId(22L);
        PageImpl<TestRun> expected = new PageImpl<>(List.of(new TestRun()), pageable, 1);
        when(accessControlService.isAdmin(any())).thenReturn(false);
        when(accessControlService.currentUserEmail(any())).thenReturn("qc@merchant.com");
        when(sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc("qc@merchant.com"))
                .thenReturn(List.of(ownedSession));
        when(testRunRepository.findBySessionIdInOrderByCreatedAtDesc(List.of(22L), pageable)).thenReturn(expected);

        assertThat(testExecutionService.findHistory(null, null, pageable, merchantPrincipal()))
                .isSameAs(expected);
        verify(testRunRepository).findBySessionIdInOrderByCreatedAtDesc(List.of(22L), pageable);
        verify(testRunRepository, never()).findAllByOrderByCreatedAtDesc(pageable);
    }

    private SitUserPrincipal adminPrincipal() {
        SitUser user = new SitUser();
        user.setId(1L);
        user.setEmail("admin@test.com");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        return new SitUserPrincipal(user);
    }

    private SitUserPrincipal merchantPrincipal() {
        SitUser user = new SitUser();
        user.setId(2L);
        user.setEmail("qc@merchant.com");
        user.setRole(UserRole.MERCHANT_QC);
        user.setActive(true);
        return new SitUserPrincipal(user);
    }

    private TestRun runForCase(TestCaseType testCase, boolean passed, String actualRsp, long id) {
        TestRun run = new TestRun();
        run.setId(id);
        run.setPartnerName("Demo Merchant");
        run.setTxnRef("TXN_SUITE");
        run.setCallbackType(CallbackType.IPN);
        run.setTestCase(testCase);
        run.setExpectedRspCode(testCase.getExpectedRspCode());
        run.setActualRspCode(actualRsp);
        run.setPassed(passed);
        run.setHttpStatus(200);
        return run;
    }

    private TestRunForm ipnForm(TestCaseType testCase) {
        TestRunForm form = new TestRunForm();
        form.setPartnerId(1L);
        form.setSessionId(2L);
        form.setCallbackType(CallbackType.IPN);
        form.setTestCase(testCase);
        form.setTxnRef("TXN001");
        form.setAmountVnd(100_000L);
        return form;
    }

    private static CallbackHttpRunner.CallbackResponse callbackResponse(int status, String body) {
        return new CallbackHttpRunner.CallbackResponse(
                "http://merchant.test/callback",
                status,
                body,
                12L,
                null
        );
    }
}
