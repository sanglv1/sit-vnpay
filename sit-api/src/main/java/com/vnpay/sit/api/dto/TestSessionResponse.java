package com.vnpay.sit.api.dto;

import com.vnpay.sit.session.entity.TestSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestSessionResponse {
    private final Long id;
    private final Long partnerId;
    private final String partnerName;
    private final String tmnCode;
    private final String note;
    private final String status;
    private final int autoPassed;
    private final int autoTotal;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static TestSessionResponse from(TestSession entity, int autoPassed, int autoTotal) {
        return TestSessionResponse.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartnerName())
                .tmnCode(entity.getTmnCode())
                .note(entity.getNote())
                .status(entity.getStatus())
                .autoPassed(autoPassed)
                .autoTotal(autoTotal)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
