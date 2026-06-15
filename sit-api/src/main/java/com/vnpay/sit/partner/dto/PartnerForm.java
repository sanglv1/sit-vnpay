package com.vnpay.sit.partner.dto;

import com.vnpay.sit.model.PaymentFlow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerForm {

    private Long id;

    @NotBlank(message = "Tên đối tác không được để trống")
    private String name;

    @NotNull(message = "Luồng thanh toán không được để trống")
    private PaymentFlow flow;

    @NotBlank(message = "TMN Code không được để trống")
    private String tmnCode;

    @NotBlank(message = "Secret Key không được để trống")
    private String secretKey;

    private String returnUrl;

    @NotBlank(message = "IPN URL không được để trống")
    private String ipnUrl;

    private String note;

    private boolean active = true;
}
