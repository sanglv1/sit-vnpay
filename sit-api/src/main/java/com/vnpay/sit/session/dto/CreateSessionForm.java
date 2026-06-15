package com.vnpay.sit.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionForm {

    @NotNull(message = "Chọn đối tác")
    private Long partnerId;

    private String note;
}
