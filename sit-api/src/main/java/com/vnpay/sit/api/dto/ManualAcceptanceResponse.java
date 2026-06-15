package com.vnpay.sit.api.dto;

import com.vnpay.sit.manual.entity.ManualAcceptance;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ManualAcceptanceResponse {
    private final Long id;
    private final Long partnerId;
    private final String partnerName;
    private final String returnSuccessTxnRef;
    private final String returnSuccessImage;
    private final String returnFailedTxnRef;
    private final String returnFailedImage;
    private final Boolean exceptionHandled;
    private final Boolean whitelistIpPassed;
    private final Boolean logStoragePassed;
    private final String note;
    private final LocalDateTime updatedAt;

    public static ManualAcceptanceResponse from(ManualAcceptance entity) {
        return ManualAcceptanceResponse.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartnerName())
                .returnSuccessTxnRef(entity.getReturnSuccessTxnRef())
                .returnSuccessImage(entity.getReturnSuccessImage())
                .returnFailedTxnRef(entity.getReturnFailedTxnRef())
                .returnFailedImage(entity.getReturnFailedImage())
                .exceptionHandled(entity.getExceptionHandled())
                .whitelistIpPassed(entity.getWhitelistIpPassed())
                .logStoragePassed(entity.getLogStoragePassed())
                .note(entity.getNote())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
