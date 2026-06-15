package com.vnpay.sit.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardChartDay {
    private final String label;
    private final long passed;
    private final long failed;
}
