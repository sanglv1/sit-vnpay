package com.vnpay.sit.testrun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.core.CallbackFields;
import com.vnpay.sit.core.CallbackParamBuilder;
import com.vnpay.sit.core.CallbackSigner;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.TokenIpnCommand;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.runner.CallbackHttpRunner;
import com.vnpay.sit.api.dto.PrepareOrderResponse;
import com.vnpay.sit.api.dto.TestRunResponse;
import com.vnpay.sit.api.dto.TestSuiteResponse;
import com.vnpay.sit.api.dto.TestSuiteStepResponse;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.session.service.TestSessionService;
import com.vnpay.sit.testrun.dto.PrepareMerchantOrderForm;
import com.vnpay.sit.testrun.dto.TestSuiteForm;
import com.vnpay.sit.testrun.dto.TestRunForm;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TestExecutionService {

    private static final Pattern RSP_CODE_PATTERN = Pattern.compile(
            "\"RspCode\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private final PartnerService partnerService;
    private final TestRunRepository testRunRepository;
    private final CallbackHttpRunner httpRunner;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final TestSessionRepository sessionRepository;
    private final TestSessionService testSessionService;

    public TestExecutionService(
            PartnerService partnerService,
            TestRunRepository testRunRepository,
            CallbackHttpRunner httpRunner,
            ObjectMapper objectMapper,
            AccessControlService accessControlService,
            TestSessionRepository sessionRepository,
            TestSessionService testSessionService
    ) {
        this.partnerService = partnerService;
        this.testRunRepository = testRunRepository;
        this.httpRunner = httpRunner;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.sessionRepository = sessionRepository;
        this.testSessionService = testSessionService;
    }

    public Page<TestRun> findHistory(Long sessionId, String createdByEmail, Pageable pageable, SitUserPrincipal principal) {
        if (sessionId != null) {
            accessControlService.requireSessionAccess(sessionId, principal);
            return testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
        }
        if (accessControlService.isAdmin(principal)) {
            if (StringUtils.hasText(createdByEmail)) {
                return findHistoryForCreator(createdByEmail.trim(), pageable);
            }
            return testRunRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return findHistoryForMerchant(pageable, principal);
    }

    public TestRunResponse toResponse(TestRun run) {
        String sessionCreatedByEmail = null;
        if (run.getSessionId() != null) {
            sessionCreatedByEmail = sessionRepository.findById(run.getSessionId())
                    .map(TestSession::getCreatedByEmail)
                    .orElse(null);
        }
        return TestRunResponse.from(run, sessionCreatedByEmail);
    }

    public List<TestRunResponse> toResponses(List<TestRun> runs) {
        if (runs.isEmpty()) {
            return List.of();
        }
        List<Long> sessionIds = runs.stream()
                .map(TestRun::getSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> creators = sessionIds.isEmpty()
                ? Map.of()
                : sessionRepository.findAllById(sessionIds).stream()
                        .collect(Collectors.toMap(TestSession::getId, TestSession::getCreatedByEmail));
        return runs.stream()
                .map(run -> TestRunResponse.from(
                        run,
                        run.getSessionId() != null ? creators.get(run.getSessionId()) : null))
                .toList();
    }

    private Page<TestRun> findHistoryForCreator(String email, Pageable pageable) {
        List<Long> sessionIds = sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(TestSession::getId)
                .toList();
        if (sessionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return testRunRepository.findBySessionIdInOrderByCreatedAtDesc(sessionIds, pageable);
    }

    public Optional<TestRun> findById(Long id, SitUserPrincipal principal) {
        Optional<TestRun> run = testRunRepository.findById(id);
        run.ifPresent(value -> accessControlService.requireTestRunAccess(value, principal));
        return run;
    }

    private Page<TestRun> findHistoryForMerchant(Pageable pageable, SitUserPrincipal principal) {
        String email = accessControlService.currentUserEmail(principal);
        List<Long> sessionIds = sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(TestSession::getId)
                .toList();
        if (sessionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return testRunRepository.findBySessionIdInOrderByCreatedAtDesc(sessionIds, pageable);
    }

    @Transactional
    public TestRun execute(TestRunForm form, SitUserPrincipal principal) {
        requireRunnableSession(form.getSessionId(), principal);
        PartnerConfig partner = partnerService.requireAccessible(form.getPartnerId(), principal);

        Map<String, String> params = buildParams(partner, form);

        String targetUrl = form.getCallbackType() == CallbackType.IPN
                ? partner.getIpnUrl()
                : partner.getReturnUrl();
        if (targetUrl == null || targetUrl.isBlank()) {
            String field = form.getCallbackType() == CallbackType.IPN ? "IPN URL" : "Return URL";
            throw new IllegalArgumentException("Chưa cấu hình " + field + " cho đối tác");
        }

        boolean asIpn = form.getCallbackType() == CallbackType.IPN;
        CallbackHttpRunner.CallbackResponse response = httpRunner.execute(targetUrl, params, asIpn);

        TestRun run = new TestRun();
        run.setPartnerId(partner.getId());
        run.setSessionId(form.getSessionId());
        run.setPartnerName(partner.getName());
        run.setFlow(partner.getFlow());
        run.setCallbackType(form.getCallbackType());
        run.setTestCase(form.getTestCase());
        run.setTxnRef(form.getTxnRef().trim());
        run.setTargetUrl(targetUrl);
        run.setRequestParams(toJson(params));
        run.setRequestUrl(truncate(response.requestUrl(), 8000));
        run.setHttpStatus(response.httpStatus());
        run.setResponseBody(truncate(response.responseBody(), 8000));
        run.setDurationMs(response.durationMs());
        run.setErrorMessage(response.errorMessage());

        if (asIpn) {
            String actualRsp = extractRspCode(response.responseBody());
            run.setActualRspCode(actualRsp);
            run.setExpectedRspCode(form.getTestCase().getExpectedRspCode());
            run.setPassed(evaluateIpn(form.getTestCase(), response, actualRsp));
        } else {
            run.setPassed(response.httpStatus() >= 200 && response.httpStatus() < 400 && !response.hasError());
        }

        TestRun saved = testRunRepository.save(run);
        testSessionService.mergeTestInputFromRun(
                form.getSessionId(),
                form.getTestCase(),
                form.getTxnRef().trim(),
                form.getAmountVnd(),
                form.getWrongAmountVnd(),
                principal
        );
        return saved;
    }

    @Transactional
    public TestSuiteResponse executeIpnSuite(TestSuiteForm form, SitUserPrincipal principal) {
        requireRunnableSession(form.getSessionId(), principal);
        PartnerConfig partner = partnerService.requireAccessible(form.getPartnerId(), principal);
        requireDistinctSuccessAndFailedTxnRef(form.getTxnRef(), form.getFailedTxnRef());

        long wrongAmount = form.getWrongAmountVnd() != null
                ? form.getWrongAmountVnd()
                : form.getAmountVnd() + 1_000;

        List<TestCaseType> cases = TestCaseType.ipnSuiteExecutionOrder();
        List<TestSuiteStepResponse> steps = new ArrayList<>();

        int step = 1;
        for (TestCaseType testCase : cases) {
            TestRunForm runForm = new TestRunForm();
            runForm.setPartnerId(form.getPartnerId());
            runForm.setSessionId(form.getSessionId());
            runForm.setCallbackType(CallbackType.IPN);
            runForm.setTestCase(testCase);
            applySuiteOrder(runForm, form, testCase);
            if (testCase == TestCaseType.WRONG_AMOUNT) {
                runForm.setWrongAmountVnd(wrongAmount);
            }
            runForm.setRecurringIpnCommand(form.getRecurringIpnCommand());
            runForm.setTokenIpnCommand(form.getTokenIpnCommand());
            TestRun run = execute(runForm, principal);
            steps.add(TestSuiteStepResponse.from(step++, testCase, run));
        }

        int passed = (int) steps.stream().filter(TestSuiteStepResponse::isPassed).count();
        return TestSuiteResponse.builder()
                .txnRef(form.getTxnRef().trim())
                .failedTxnRef(form.getFailedTxnRef().trim())
                .partnerName(partner.getName())
                .sessionId(form.getSessionId())
                .totalSteps(steps.size())
                .passedSteps(passed)
                .allPassed(passed == steps.size())
                .steps(steps)
                .build();
    }

    public List<TestRunResponse> findLatestRunsForSession(Long sessionId, SitUserPrincipal principal) {
        accessControlService.requireSessionAccess(sessionId, principal);
        return toResponses(findEffectiveRunsForSession(sessionId));
    }

    private List<TestRun> findEffectiveRunsForSession(Long sessionId) {
        return TestRunGrouping.effectiveRunsList(
                testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId),
                run -> true
        );
    }

    private Map<TestCaseType, TestRun> latestAutoIpnRunsByCase(Long sessionId) {
        Map<TestCaseType, TestRun> effective = TestRunGrouping.effectiveByTestCase(
                testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId),
                run -> run.getCallbackType() == CallbackType.IPN
                        && TestCaseType.ipnSuiteExecutionOrder().contains(run.getTestCase())
        );
        return new LinkedHashMap<>(effective);
    }

    public Optional<TestSuiteResponse> getIpnSuiteResult(Long sessionId, SitUserPrincipal principal) {
        accessControlService.requireSessionAccess(sessionId, principal);
        return getIpnSuiteResult(sessionId);
    }

    public Optional<TestSuiteResponse> getIpnSuiteResult(Long sessionId) {
        Map<TestCaseType, TestRun> latestByCase = latestAutoIpnRunsByCase(sessionId);
        if (latestByCase.isEmpty()) {
            return Optional.empty();
        }

        String partnerName = latestByCase.values().iterator().next().getPartnerName();
        String txnRef = null;
        String failedTxnRef = null;
        List<TestSuiteStepResponse> steps = new ArrayList<>();
        int stepNum = 1;
        for (TestCaseType testCase : TestCaseType.ipnSuiteExecutionOrder()) {
            TestRun run = latestByCase.get(testCase);
            if (run == null) {
                steps.add(TestSuiteStepResponse.builder()
                        .step(stepNum++)
                        .caseCode(testCase.getCaseCode())
                        .checkOrder(testCase.getCheckOrder())
                        .testCase(testCase)
                        .testCaseLabel(testCase.getLabel())
                        .expectedRspCode(testCase.getExpectedRspCode())
                        .passed(false)
                        .build());
            } else {
                if (testCase == TestCaseType.FAILED) {
                    failedTxnRef = run.getTxnRef();
                } else if (txnRef == null && run.getTxnRef() != null
                        && testCase != TestCaseType.INVALID_HASH
                        && testCase != TestCaseType.ORDER_NOT_FOUND) {
                    txnRef = run.getTxnRef();
                }
                steps.add(TestSuiteStepResponse.from(stepNum++, testCase, run));
            }
        }

        int total = TestCaseType.ipnSuiteExecutionOrder().size();
        int passed = (int) steps.stream().filter(TestSuiteStepResponse::isPassed).count();
        return Optional.of(TestSuiteResponse.builder()
                .sessionId(sessionId)
                .txnRef(txnRef != null ? txnRef : "")
                .failedTxnRef(failedTxnRef != null ? failedTxnRef : "")
                .partnerName(partnerName != null ? partnerName : "")
                .totalSteps(total)
                .passedSteps(passed)
                .allPassed(passed == total)
                .steps(steps)
                .build());
    }

    private static void applySuiteOrder(TestRunForm runForm, TestSuiteForm form, TestCaseType testCase) {
        if (testCase == TestCaseType.FAILED) {
            runForm.setTxnRef(form.getFailedTxnRef().trim());
            runForm.setAmountVnd(form.getFailedAmountVnd() != null ? form.getFailedAmountVnd() : form.getAmountVnd());
            return;
        }
        runForm.setTxnRef(form.getTxnRef().trim());
        runForm.setAmountVnd(form.getAmountVnd());
    }

    private static void requireDistinctSuccessAndFailedTxnRef(String successTxnRef, String failedTxnRef) {
        if (!StringUtils.hasText(failedTxnRef)) {
            throw new IllegalArgumentException("Nhập mã giao dịch thất bại (Case 6)");
        }
        if (successTxnRef.trim().equalsIgnoreCase(failedTxnRef.trim())) {
            throw new IllegalArgumentException("txnRef Case 5 (thành công) và Case 6 (thất bại) phải khác nhau");
        }
    }

    public PrepareOrderResponse prepareMerchantOrder(PrepareMerchantOrderForm form, SitUserPrincipal principal) {
        PartnerConfig partner = partnerService.requireAccessible(form.getPartnerId(), principal);

        String ipnUrl = partner.getIpnUrl();
        if (ipnUrl == null || ipnUrl.isBlank()) {
            throw new IllegalArgumentException("Chưa cấu hình IPN URL cho đối tác");
        }

        String prepareUrl = resolvePrepareUrl(ipnUrl);
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(form.getAmountVnd()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(prepareUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("Merchant không phản hồi prepare-order (HTTP "
                        + response.getStatusCode().value() + ")");
            }
            JsonNode node = objectMapper.readTree(response.getBody());
            String txnRef = node.path("txnRef").asText(null);
            if (txnRef == null || txnRef.isBlank()) {
                throw new IllegalArgumentException(
                        "Merchant chưa hỗ trợ prepare-order — tạo 2 GD trên merchant (dừng OTP), nhập txnRef vào form SIT");
            }
            long amountVnd = node.path("amountVnd").asLong(form.getAmountVnd());
            return PrepareOrderResponse.builder()
                    .txnRef(txnRef)
                    .amountVnd(amountVnd)
                    .prepareUrl(prepareUrl)
                    .build();
        } catch (RestClientException ex) {
            throw new IllegalArgumentException(
                    "Không gọi được " + prepareUrl + ". Tạo đơn thủ công trên merchant (đến OTP, copy txnRef) rồi nhập vào form SIT. "
                            + ex.getMessage());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Phản hồi prepare-order không phải JSON hợp lệ");
        }
    }

    private static String resolvePrepareUrl(String ipnUrl) {
        URI uri = URI.create(ipnUrl.trim());
        String base = uri.getScheme() + "://" + uri.getAuthority();
        return base + "/api/sit/prepare-order";
    }

    private void requireRunnableSession(Long sessionId, SitUserPrincipal principal) {
        if (sessionId == null) {
            if (!accessControlService.isAdmin(principal)) {
                throw new IllegalArgumentException("Phải chọn phiên kiểm thử khi chạy test");
            }
            return;
        }
        accessControlService.requireSessionAccess(sessionId, principal);
    }

    private Map<String, String> buildParams(PartnerConfig partner, TestRunForm form) {
        if (form.getTestCase() == TestCaseType.UNKNOWN_ERROR) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put(CallbackFields.tmnCodeKey(partner.getFlow()), partner.getTmnCode());
            return params;
        }

        Map<String, String> params = CallbackParamBuilder.build(
                partner.getFlow(),
                form.getTestCase(),
                partner.getTmnCode(),
                form.getTxnRef().trim(),
                form.getAmountVnd(),
                form.getWrongAmountVnd(),
                resolveRecurringCommand(partner.getFlow(), form.getRecurringIpnCommand()),
                resolveTokenCommand(partner.getFlow(), form.getTokenIpnCommand())
        );

        if (form.getTestCase() == TestCaseType.INVALID_HASH) {
            params.put(CallbackSigner.hashFieldFor(partner.getFlow()), "invalid_sit_hash");
        } else {
            CallbackSigner.attachHash(params, partner.getSecretKey(), partner.getFlow());
        }
        return params;
    }

    private static RecurringIpnCommand resolveRecurringCommand(
            PaymentFlow flow,
            RecurringIpnCommand recurringIpnCommand
    ) {
        if (flow != PaymentFlow.RECURRING) {
            return null;
        }
        return recurringIpnCommand != null ? recurringIpnCommand : RecurringIpnCommand.defaultForIpnSuite();
    }

    private static TokenIpnCommand resolveTokenCommand(PaymentFlow flow, TokenIpnCommand tokenIpnCommand) {
        if (flow != PaymentFlow.TOKEN) {
            return null;
        }
        return tokenIpnCommand != null ? tokenIpnCommand : TokenIpnCommand.defaultForIpnSuite();
    }

    private boolean evaluateIpn(TestCaseType testCase, CallbackHttpRunner.CallbackResponse response, String actualRsp) {
        if (response.hasError() || response.httpStatus() < 200 || response.httpStatus() >= 400) {
            return false;
        }
        String expected = testCase.getExpectedRspCode();
        if (expected == null) {
            return actualRsp != null;
        }
        return expected.equals(actualRsp);
    }

    private String extractRspCode(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = RSP_CODE_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode rsp = node.get("RspCode");
            if (rsp == null) {
                rsp = node.get("rspCode");
            }
            return rsp != null ? rsp.asText() : null;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return params.toString();
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
