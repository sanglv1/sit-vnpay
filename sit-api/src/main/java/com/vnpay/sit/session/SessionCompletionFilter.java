package com.vnpay.sit.session;

public enum SessionCompletionFilter {
    ALL,
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED;

    public static SessionCompletionFilter fromParam(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return SessionCompletionFilter.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ALL;
        }
    }
}
