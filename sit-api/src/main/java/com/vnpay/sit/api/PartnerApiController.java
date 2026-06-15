package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.PartnerResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.dto.PartnerForm;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
                ? partnerService.findAllActive()
                : partnerService.findAll();
        boolean includeSecret = accessControlService.isAdmin(principal);
        List<PartnerResponse> data = partners.stream()
                .map(p -> PartnerResponse.from(p, includeSecret))
                .toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        PartnerConfig partner = partnerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));
        return ApiResponse.ok(PartnerResponse.from(partner, accessControlService.isAdmin(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PartnerResponse> create(@Valid @RequestBody PartnerForm form) {
        form.setId(null);
        PartnerConfig saved = partnerService.save(form);
        return ApiResponse.ok(PartnerResponse.from(saved, true));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PartnerResponse> update(@PathVariable Long id, @Valid @RequestBody PartnerForm form) {
        form.setId(id);
        PartnerConfig saved = partnerService.save(form);
        return ApiResponse.ok(PartnerResponse.from(saved, true));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        partnerService.delete(id);
        return ApiResponse.ok(null);
    }
}
