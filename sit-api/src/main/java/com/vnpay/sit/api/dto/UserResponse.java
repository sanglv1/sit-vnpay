package com.vnpay.sit.api.dto;

import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.user.entity.SitUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private final Long id;
    private final String fullName;
    private final String email;
    private final UserRole role;
    private final String roleLabel;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static UserResponse from(SitUser entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .roleLabel(entity.getRole().getLabel())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
