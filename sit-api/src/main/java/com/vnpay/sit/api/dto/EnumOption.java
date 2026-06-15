package com.vnpay.sit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnumOption {
    private final String value;
    private final String label;
    private final String expectedRspCode;
    private final String caseCode;
    private final int checkOrder;
}
