package com.vnpay.sit.api.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    public static final String SUCCESS = "00";

    private final String code;
    private final T data;
    private final String rspMsg;

    private ApiResponse(String code, T data, String rspMsg) {
        this.code = code;
        this.data = data;
        this.rspMsg = rspMsg;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS, data, "Success");
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
