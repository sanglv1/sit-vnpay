package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.DashboardResponse;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.dashboard.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> dashboard(@AuthenticationPrincipal SitUserPrincipal principal) {
        return ApiResponse.ok(dashboardService.build(principal));
    }
}
