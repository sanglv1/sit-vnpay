package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.*;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.TokenIpnCommand;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.SessionCompletionFilter;
import com.vnpay.sit.session.dto.CreateSessionForm;
import com.vnpay.sit.session.dto.SaveSessionTestInputForm;
import com.vnpay.sit.session.service.TestSessionService;
import com.vnpay.sit.export.MinutesExportService;
import com.vnpay.sit.testrun.service.TestExecutionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class TestSessionApiController {

    private final TestSessionService testSessionService;
    private final MinutesExportService minutesExportService;
    private final TestExecutionService testExecutionService;
    private final AccessControlService accessControlService;
    private final PartnerService partnerService;

    public TestSessionApiController(
            TestSessionService testSessionService,
            MinutesExportService minutesExportService,
            TestExecutionService testExecutionService,
            AccessControlService accessControlService,
            PartnerService partnerService
    ) {
        this.testSessionService = testSessionService;
        this.minutesExportService = minutesExportService;
        this.testExecutionService = testExecutionService;
        this.accessControlService = accessControlService;
        this.partnerService = partnerService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TestSessionResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String completion,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        Page<TestSessionResponse> sessions = testSessionService.findAll(
                PageRequest.of(page, size),
                q,
                SessionCompletionFilter.fromParam(completion),
                principal
        );
        PageResponse<TestSessionResponse> data = new PageResponse<>(
                sessions.getContent(),
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<TestSessionResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        return ApiResponse.ok(testSessionService.getById(id, principal));
    }

    @PostMapping
    public ApiResponse<TestSessionResponse> create(
            @Valid @RequestBody CreateSessionForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        String email = accessControlService.currentUserEmail(principal);
        return ApiResponse.ok(testSessionService.create(form, email, principal));
    }

    @PatchMapping("/{id}/test-input")
    public ResponseEntity<Void> saveTestInput(
            @PathVariable Long id,
            @Valid @RequestBody SaveSessionTestInputForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        testSessionService.saveTestInput(id, form, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/workspace")
    public ApiResponse<SessionWorkspaceResponse> workspace(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        TestSessionResponse session = testSessionService.getById(id, principal);
        PartnerConfig partner = partnerService.requireAccessible(session.getPartnerId(), principal);
        PaymentFlow partnerFlow = partner.getFlow();
        return ApiResponse.ok(SessionWorkspaceResponse.builder()
                .session(session)
                .partnerFlow(partnerFlow)
                .latestRuns(testExecutionService.findLatestRunsForSession(id, principal))
                .testCases(toTestCaseOptions())
                .recurringIpnCommands(partnerFlow == PaymentFlow.RECURRING
                        ? toRecurringCommandOptions()
                        : java.util.List.of())
                .tokenIpnCommands(partnerFlow == PaymentFlow.TOKEN
                        ? toTokenCommandOptions()
                        : java.util.List.of())
                .build());
    }

    @GetMapping("/{id}/latest-runs")
    public ApiResponse<java.util.List<TestRunResponse>> latestRuns(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        return ApiResponse.ok(testExecutionService.findLatestRunsForSession(id, principal));
    }

    @GetMapping("/{id}/suite-result")
    public ApiResponse<TestSuiteResponse> suiteResult(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        TestSuiteResponse result = testExecutionService.getIpnSuiteResult(id, principal)
                .orElseThrow(() -> new IllegalArgumentException("Chưa có kết quả suite IPN cho phiên này"));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}/export-minutes")
    public ResponseEntity<byte[]> exportMinutes(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal,
            @RequestParam(required = false) String vnpayRepresentative,
            @RequestParam(required = false) String merchantRepresentative,
            @RequestParam(required = false) String websiteName,
            @RequestParam(required = false) String testLink,
            @RequestParam(required = false) String integrationVersion
    ) {
        accessControlService.requireSessionAccess(id, principal);
        MinutesExportService.ExportedMinutes exported = minutesExportService.export(
                id,
                vnpayRepresentative,
                merchantRepresentative,
                websiteName,
                testLink,
                integrationVersion
        );
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(exported.fileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(exported.content());
    }

    private static java.util.List<EnumOption> toTestCaseOptions() {
        return TestCaseType.autoIpnTestCases().stream()
                .map(v -> new EnumOption(v.name(), v.getLabel(), v.getExpectedRspCode(),
                        v.getCaseCode(), v.getCheckOrder()))
                .toList();
    }

    private static java.util.List<EnumOption> toRecurringCommandOptions() {
        return java.util.Arrays.stream(RecurringIpnCommand.values())
                .map(v -> new EnumOption(v.name(), v.getLabel(), v.getCommandValue(), null, 0))
                .toList();
    }

    private static java.util.List<EnumOption> toTokenCommandOptions() {
        return java.util.Arrays.stream(TokenIpnCommand.values())
                .map(v -> new EnumOption(v.name(), v.getLabel(), v.getCommandValue(), null, 0))
                .toList();
    }
}
