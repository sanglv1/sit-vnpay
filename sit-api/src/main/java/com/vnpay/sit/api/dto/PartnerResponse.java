package com.vnpay.sit.api.dto;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.partner.entity.PartnerConfig;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PartnerResponse {
    private final Long id;
    private final String name;
    private final PaymentFlow flow;
    private final String flowLabel;
    private final String tmnCode;
    private final String secretKey;
    private final String returnUrl;
    private final String ipnUrl;
    private final String note;
    private final boolean active;
    private final String createdByEmail;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PartnerResponse from(PartnerConfig entity, boolean includeSecret) {
        return PartnerResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .flow(entity.getFlow())
                .flowLabel(entity.getFlow().getLabel())
                .tmnCode(entity.getTmnCode())
                .secretKey(includeSecret ? entity.getSecretKey() : maskSecret(entity.getSecretKey()))
                .returnUrl(entity.getReturnUrl())
                .ipnUrl(entity.getIpnUrl())
                .note(entity.getNote())
                .active(entity.isActive())
                .createdByEmail(entity.getCreatedByEmail())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }
}
