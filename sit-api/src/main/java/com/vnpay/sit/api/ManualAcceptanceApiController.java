package com.vnpay.sit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.ManualAcceptanceResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.manual.dto.ManualAcceptanceForm;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.service.ManualAcceptanceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manual-acceptance")
public class ManualAcceptanceApiController {

    private final ManualAcceptanceService manualAcceptanceService;
    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    public ManualAcceptanceApiController(
            ManualAcceptanceService manualAcceptanceService,
            AccessControlService accessControlService,
            ObjectMapper objectMapper
    ) {
        this.manualAcceptanceService = manualAcceptanceService;
        this.accessControlService = accessControlService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/latest")
    public ApiResponse<ManualAcceptanceResponse> latest(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long sessionId,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        ManualAcceptance entity = null;
        if (sessionId != null) {
            accessControlService.requireSessionAccess(sessionId, principal);
            entity = manualAcceptanceService.findLatestBySession(sessionId).orElse(null);
        } else if (partnerId != null) {
            accessControlService.requireAdmin(principal);
            entity = manualAcceptanceService.findLatestByPartner(partnerId).orElse(null);
        }
        return ApiResponse.ok(entity != null ? ManualAcceptanceResponse.from(entity, objectMapper) : null);
    }

    @PostMapping
    public ApiResponse<ManualAcceptanceResponse> save(
            @Valid @RequestBody ManualAcceptanceForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        if (form.getSessionId() != null) {
            accessControlService.requireSessionAccess(form.getSessionId(), principal);
        } else {
            accessControlService.requireAdmin(principal);
        }
        ManualAcceptance saved = manualAcceptanceService.save(form, principal);
        return ApiResponse.ok(ManualAcceptanceResponse.from(saved, objectMapper));
    }
}
