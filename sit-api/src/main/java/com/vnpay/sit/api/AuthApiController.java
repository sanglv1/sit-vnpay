package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.AuthResponse;
import com.vnpay.sit.api.dto.UserResponse;
import com.vnpay.sit.auth.AuthService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal SitUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }
        return ApiResponse.ok(authService.currentUser(principal));
    }
}
