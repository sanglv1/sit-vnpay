package com.vnpay.sit.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.InstalmentManualEvidenceSupport;
import com.vnpay.sit.manual.RecurringManualEvidenceSupport;
import com.vnpay.sit.manual.TokenManualEvidenceSupport;
import com.vnpay.sit.manual.dto.TokenScenarioEvidence;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

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
    private final Map<String, TokenScenarioEvidence> tokenScenarioEvidence;
    private final Map<String, TokenScenarioEvidence> recurringScenarioEvidence;
    private final Map<String, TokenScenarioEvidence> instalmentScenarioEvidence;
    private final LocalDateTime updatedAt;

    public static ManualAcceptanceResponse from(ManualAcceptance entity, ObjectMapper objectMapper) {
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
                .tokenScenarioEvidence(TokenManualEvidenceSupport.toApiMap(
                        TokenManualEvidenceSupport.withLegacyPayAndCreate(
                                TokenManualEvidenceSupport.parse(entity.getTokenScenarioEvidence(), objectMapper),
                                entity
                        )
                ))
                .recurringScenarioEvidence(RecurringManualEvidenceSupport.toApiMap(
                        RecurringManualEvidenceSupport.withLegacyCardVerify(
                                RecurringManualEvidenceSupport.parse(entity.getRecurringScenarioEvidence(), objectMapper),
                                entity
                        )
                ))
                .instalmentScenarioEvidence(InstalmentManualEvidenceSupport.toApiMap(
                        InstalmentManualEvidenceSupport.withLegacyPayReturn(
                                InstalmentManualEvidenceSupport.parse(entity.getInstalmentScenarioEvidence(), objectMapper),
                                entity
                        )
                ))
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
