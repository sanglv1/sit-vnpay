package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.PageResponse;
import com.vnpay.sit.api.dto.TestSessionResponse;
import com.vnpay.sit.api.dto.TestSuiteResponse;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.session.dto.CreateSessionForm;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
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

    public TestSessionApiController(
            TestSessionService testSessionService,
            MinutesExportService minutesExportService,
            TestExecutionService testExecutionService,
            AccessControlService accessControlService
    ) {
        this.testSessionService = testSessionService;
        this.minutesExportService = minutesExportService;
        this.testExecutionService = testExecutionService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TestSessionResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        Page<TestSessionResponse> sessions = testSessionService.findAll(PageRequest.of(page, size), principal);
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
        String email = principal != null ? principal.getUsername() : null;
        return ApiResponse.ok(testSessionService.create(form, email));
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
}
