package com.vnpay.sit.model;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ADMIN"),
    MERCHANT_QC("MERCHANT QC");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }
}
