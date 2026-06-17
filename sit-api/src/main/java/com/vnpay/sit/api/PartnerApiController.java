package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.PartnerResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.dto.PartnerForm;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
public class PartnerApiController {

    private final PartnerService partnerService;
    private final AccessControlService accessControlService;

    public PartnerApiController(PartnerService partnerService, AccessControlService accessControlService) {
        this.partnerService = partnerService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ApiResponse<List<PartnerResponse>> list(
            @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        List<PartnerConfig> partners = Boolean.TRUE.equals(active)
                ? partnerService.findAllActiveForPrincipal(principal)
                : partnerService.findAllForPrincipal(principal);
        List<PartnerResponse> data = partners.stream()
                .map(p -> PartnerResponse.from(p, accessControlService.canViewPartnerSecret(p, principal)))
                .toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        PartnerConfig partner = partnerService.requireAccessible(id, principal);
        return ApiResponse.ok(PartnerResponse.from(
                partner, accessControlService.canViewPartnerSecret(partner, principal)));
    }

    @PostMapping
    public ApiResponse<PartnerResponse> create(
            @Valid @RequestBody PartnerForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        form.setId(null);
        PartnerConfig saved = partnerService.save(form, principal);
        return ApiResponse.ok(PartnerResponse.from(
                saved, accessControlService.canViewPartnerSecret(saved, principal)));
    }

    @PutMapping("/{id}")
    public ApiResponse<PartnerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PartnerForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        form.setId(id);
        PartnerConfig saved = partnerService.save(form, principal);
        return ApiResponse.ok(PartnerResponse.from(
                saved, accessControlService.canViewPartnerSecret(saved, principal)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        partnerService.delete(id, principal);
        return ApiResponse.ok(null);
    }
}
