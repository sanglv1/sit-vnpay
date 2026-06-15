package com.vnpay.sit.testrun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.core.CallbackParamBuilder;
import com.vnpay.sit.core.CallbackSigner;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.runner.CallbackHttpRunner;
import com.vnpay.sit.api.dto.PrepareOrderResponse;
import com.vnpay.sit.api.dto.TestSuiteResponse;
import com.vnpay.sit.api.dto.TestSuiteStepResponse;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public TestExecutionService(
            PartnerService partnerService,
            TestRunRepository testRunRepository,
            CallbackHttpRunner httpRunner,
            ObjectMapper objectMapper,
            AccessControlService accessControlService,
            TestSessionRepository sessionRepository
    ) {
        this.partnerService = partnerService;
        this.testRunRepository = testRunRepository;
        this.httpRunner = httpRunner;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.sessionRepository = sessionRepository;
    }

    public Page<TestRun> findHistory(Pageable pageable, SitUserPrincipal principal) {
        if (accessControlService.isAdmin(principal)) {
            return testRunRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return findHistoryForMerchant(pageable, principal);
    }

    public Page<TestRun> findHistory(Long sessionId, Pageable pageable, SitUserPrincipal principal) {
        if (sessionId == null) {
            return findHistory(pageable, principal);
        }
        accessControlService.requireSessionAccess(sessionId, principal);
        return testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
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
        PartnerConfig partner = partnerService.findById(form.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

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

        return testRunRepository.save(run);
    }

    @Transactional
    public TestSuiteResponse executeIpnSuite(TestSuiteForm form, SitUserPrincipal principal) {
        requireRunnableSession(form.getSessionId(), principal);
        PartnerConfig partner = partnerService.findById(form.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

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
            runForm.setTxnRef(form.getTxnRef().trim());
            runForm.setAmountVnd(form.getAmountVnd());
            if (testCase == TestCaseType.WRONG_AMOUNT) {
                runForm.setWrongAmountVnd(wrongAmount);
            }
            TestRun run = execute(runForm, principal);
            steps.add(TestSuiteStepResponse.from(step++, testCase, run));
        }

        int passed = (int) steps.stream().filter(TestSuiteStepResponse::isPassed).count();
        return TestSuiteResponse.builder()
                .txnRef(form.getTxnRef().trim())
                .partnerName(partner.getName())
                .sessionId(form.getSessionId())
                .totalSteps(steps.size())
                .passedSteps(passed)
                .allPassed(passed == steps.size())
                .steps(steps)
                .build();
    }

    public Optional<TestSuiteResponse> getIpnSuiteResult(Long sessionId, SitUserPrincipal principal) {
        accessControlService.requireSessionAccess(sessionId, principal);
        return getIpnSuiteResult(sessionId);
    }

    public Optional<TestSuiteResponse> getIpnSuiteResult(Long sessionId) {
        List<TestRun> runs = testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        Map<TestCaseType, TestRun> latestByCase = new LinkedHashMap<>();
        for (TestRun run : runs) {
            if (run.getCallbackType() != CallbackType.IPN) {
                continue;
            }
            if (!TestCaseType.ipnSuiteExecutionOrder().contains(run.getTestCase())) {
                continue;
            }
            latestByCase.putIfAbsent(run.getTestCase(), run);
        }
        if (latestByCase.isEmpty()) {
            return Optional.empty();
        }

        String partnerName = latestByCase.values().iterator().next().getPartnerName();
        String txnRef = null;
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
                if (txnRef == null && run.getTxnRef() != null) {
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
                .partnerName(partnerName != null ? partnerName : "")
                .totalSteps(total)
                .passedSteps(passed)
                .allPassed(passed == total)
                .steps(steps)
                .build());
    }

    public PrepareOrderResponse prepareMerchantOrder(PrepareMerchantOrderForm form) {
        PartnerConfig partner = partnerService.findById(form.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

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
                        "Merchant chưa hỗ trợ POST /api/sit/prepare-order — tạo đơn thủ công trên merchant rồi nhập txnRef");
            }
            long amountVnd = node.path("amountVnd").asLong(form.getAmountVnd());
            return PrepareOrderResponse.builder()
                    .txnRef(txnRef)
                    .amountVnd(amountVnd)
                    .prepareUrl(prepareUrl)
                    .build();
        } catch (RestClientException ex) {
            throw new IllegalArgumentException(
                    "Không gọi được " + prepareUrl + ". Kiểm tra merchant đang chạy và có endpoint /api/sit/prepare-order. "
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
            params.put("vnp_TmnCode", partner.getTmnCode());
            return params;
        }

        Map<String, String> params = CallbackParamBuilder.build(
                partner.getFlow(),
                form.getTestCase(),
                partner.getTmnCode(),
                form.getTxnRef().trim(),
                form.getAmountVnd(),
                form.getWrongAmountVnd()
        );

        if (form.getTestCase() == TestCaseType.INVALID_HASH) {
            params.put(CallbackSigner.hashFieldFor(partner.getFlow()), "invalid_sit_hash");
        } else {
            CallbackSigner.attachHash(params, partner.getSecretKey(), partner.getFlow());
        }
        return params;
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
