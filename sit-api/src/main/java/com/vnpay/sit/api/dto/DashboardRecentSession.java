package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardRecentSession {
    private final Long id;
    private final String tmnCode;
    private final String statusLabel;
    private final boolean passed;
    private final String createdByEmail;
    private final LocalDateTime createdAt;
}
