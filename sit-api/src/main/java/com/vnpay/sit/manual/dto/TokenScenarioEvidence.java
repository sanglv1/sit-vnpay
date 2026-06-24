package com.vnpay.sit.manual.dto;

import lombok.Getter;
import lombok.Setter;

/** Bằng chứng QC thủ công cho từng tình huống Token trong biên bản (request / response / ảnh). */
@Getter
@Setter
public class TokenScenarioEvidence {

    private String requestLog;
    private String responseLog;
    private String image;

    public boolean isComplete() {
        return hasText(requestLog) && hasText(responseLog) && hasText(image);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
