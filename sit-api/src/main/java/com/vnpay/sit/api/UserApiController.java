package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.api.dto.UserResponse;
import com.vnpay.sit.user.dto.ResetPasswordForm;
import com.vnpay.sit.user.dto.UserForm;
import com.vnpay.sit.user.entity.SitUser;
import com.vnpay.sit.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(@RequestParam(required = false) String q) {
        List<UserResponse> data = userService.search(q).stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        SitUser user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserForm form) {
        form.setId(null);
        SitUser saved = userService.save(form);
        return ApiResponse.ok(UserResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserForm form) {
        form.setId(id);
        if (form.getPassword() != null && form.getPassword().isBlank()) {
            form.setPassword(null);
        }
        SitUser saved = userService.save(form);
        return ApiResponse.ok(UserResponse.from(saved));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<UserResponse> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordForm form
    ) {
        SitUser saved = userService.resetPassword(id, form);
        return ApiResponse.ok(UserResponse.from(saved));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean active = body.get("active");
        if (active == null) {
            throw new IllegalArgumentException("Thiếu trường active");
        }
        SitUser saved = userService.updateStatus(id, active);
        return ApiResponse.ok(UserResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok(null);
    }
}
