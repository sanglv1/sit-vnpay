package com.vnpay.sit.manual;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.dto.TokenScenarioEvidence;
import com.vnpay.sit.manual.entity.ManualAcceptance;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class QrDirectManualEvidenceSupport {

    private static final TypeReference<Map<String, TokenScenarioEvidence>> MAP_TYPE = new TypeReference<>() {};

    private QrDirectManualEvidenceSupport() {
    }

    public static Map<QrDirectManualScenario, TokenScenarioEvidence> parse(String json, ObjectMapper objectMapper) {
        Map<QrDirectManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            Map<String, TokenScenarioEvidence> raw = objectMapper.readValue(json, MAP_TYPE);
            if (raw == null) {
                return result;
            }
            for (Map.Entry<String, TokenScenarioEvidence> entry : raw.entrySet()) {
                try {
                    QrDirectManualScenario scenario = QrDirectManualScenario.valueOf(entry.getKey());
                    if (entry.getValue() != null) {
                        result.put(scenario, entry.getValue());
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip unknown keys
                }
            }
        } catch (JsonProcessingException ignored) {
            return emptyMap();
        }
        return result;
    }

    public static String serialize(Map<QrDirectManualScenario, TokenScenarioEvidence> evidence, ObjectMapper objectMapper) {
        Map<String, TokenScenarioEvidence> raw = new LinkedHashMap<>();
        if (evidence != null) {
            evidence.forEach((scenario, value) -> {
                if (value != null && hasAnyField(value)) {
                    raw.put(scenario.name(), value);
                }
            });
        }
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Không lưu được bằng chứng QR Direct", ex);
        }
    }

    public static Map<QrDirectManualScenario, TokenScenarioEvidence> withLegacyQrPay(
            Map<QrDirectManualScenario, TokenScenarioEvidence> evidence,
            ManualAcceptance entity
    ) {
        Map<QrDirectManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(evidence != null ? evidence : emptyMap());
        mergeLegacy(merged, QrDirectManualScenario.QR_SCAN_PAY_SUCCESS,
                entity.getReturnSuccessTxnRef(), entity.getReturnSuccessImage());
        mergeLegacy(merged, QrDirectManualScenario.QR_SCAN_PAY_FAILED,
                entity.getReturnFailedTxnRef(), entity.getReturnFailedImage());
        return merged;
    }

    public static void syncLegacyReturnFields(
            ManualAcceptance entity,
            Map<QrDirectManualScenario, TokenScenarioEvidence> evidence
    ) {
        if (evidence == null) {
            return;
        }
        TokenScenarioEvidence paySuccess = evidence.get(QrDirectManualScenario.QR_SCAN_PAY_SUCCESS);
        if (paySuccess != null) {
            if (hasText(paySuccess.getImage())) {
                entity.setReturnSuccessImage(paySuccess.getImage());
            }
            if (hasText(paySuccess.getRequestLog()) && !hasText(entity.getReturnSuccessTxnRef())) {
                entity.setReturnSuccessTxnRef(extractTxnRef(paySuccess.getRequestLog()));
            }
        }
        TokenScenarioEvidence payFailed = evidence.get(QrDirectManualScenario.QR_SCAN_PAY_FAILED);
        if (payFailed != null) {
            if (hasText(payFailed.getImage())) {
                entity.setReturnFailedImage(payFailed.getImage());
            }
            if (hasText(payFailed.getRequestLog()) && !hasText(entity.getReturnFailedTxnRef())) {
                entity.setReturnFailedTxnRef(extractTxnRef(payFailed.getRequestLog()));
            }
        }
    }

    public static Map<String, TokenScenarioEvidence> toApiMap(
            Map<QrDirectManualScenario, TokenScenarioEvidence> evidence
    ) {
        Map<String, TokenScenarioEvidence> api = new LinkedHashMap<>();
        if (evidence == null) {
            return api;
        }
        for (QrDirectManualScenario scenario : QrDirectManualScenario.values()) {
            TokenScenarioEvidence value = evidence.get(scenario);
            if (value != null && hasAnyField(value)) {
                api.put(scenario.name(), value);
            }
        }
        return api;
    }

    public static Map<QrDirectManualScenario, TokenScenarioEvidence> fromFormMap(
            Map<String, TokenScenarioEvidence> formMap
    ) {
        Map<QrDirectManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (formMap == null) {
            return result;
        }
        for (Map.Entry<String, TokenScenarioEvidence> entry : formMap.entrySet()) {
            try {
                QrDirectManualScenario scenario = QrDirectManualScenario.valueOf(entry.getKey());
                if (entry.getValue() != null) {
                    result.put(scenario, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown keys
            }
        }
        return result;
    }

    public static Map<QrDirectManualScenario, TokenScenarioEvidence> mergeEvidence(
            Map<QrDirectManualScenario, TokenScenarioEvidence> existing,
            Map<QrDirectManualScenario, TokenScenarioEvidence> incoming,
            ManualAcceptance entity
    ) {
        Map<QrDirectManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(QrDirectManualScenario.class);
        for (QrDirectManualScenario scenario : QrDirectManualScenario.values()) {
            TokenScenarioEvidence current = existing != null ? existing.get(scenario) : null;
            TokenScenarioEvidence patch = incoming != null ? incoming.get(scenario) : null;
            if (current == null && patch == null) {
                continue;
            }
            TokenScenarioEvidence next = new TokenScenarioEvidence();
            if (current != null) {
                next.setRequestLog(current.getRequestLog());
                next.setResponseLog(current.getResponseLog());
                next.setImage(current.getImage());
            }
            if (patch != null) {
                if (hasText(patch.getRequestLog())) {
                    next.setRequestLog(patch.getRequestLog().trim());
                }
                if (hasText(patch.getResponseLog())) {
                    next.setResponseLog(patch.getResponseLog().trim());
                }
                if (hasText(patch.getImage())) {
                    next.setImage(patch.getImage().trim());
                }
            }
            if (hasAnyField(next)) {
                merged.put(scenario, next);
            }
        }
        return withLegacyQrPay(merged, entity);
    }

    private static void mergeLegacy(
            Map<QrDirectManualScenario, TokenScenarioEvidence> merged,
            QrDirectManualScenario scenario,
            String txnRef,
            String image
    ) {
        TokenScenarioEvidence existing = merged.computeIfAbsent(scenario, ignored -> new TokenScenarioEvidence());
        if (!hasText(existing.getImage()) && hasText(image)) {
            existing.setImage(image);
        }
        if (!hasText(existing.getRequestLog()) && hasText(txnRef)) {
            existing.setRequestLog("vnp_TxnRef: " + txnRef.trim());
        }
    }

    public static Optional<TokenScenarioEvidence> evidence(
            ManualAcceptance manual,
            QrDirectManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (manual == null) {
            return Optional.empty();
        }
        Map<QrDirectManualScenario, TokenScenarioEvidence> map =
                withLegacyQrPay(parse(manual.getQrDirectScenarioEvidence(), objectMapper), manual);
        return Optional.ofNullable(map.get(scenario));
    }

    public static boolean passesEvaluation(
            ManualAcceptance manual,
            QrDirectManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (evidence(manual, scenario, objectMapper).map(v -> isCompleteForScenario(v, scenario)).orElse(false)) {
            return true;
        }
        if (manual == null) {
            return false;
        }
        if (scenario == QrDirectManualScenario.QR_SCAN_PAY_SUCCESS) {
            return hasText(manual.getReturnSuccessTxnRef()) && hasText(manual.getReturnSuccessImage());
        }
        if (scenario == QrDirectManualScenario.QR_SCAN_PAY_FAILED) {
            return hasText(manual.getReturnFailedTxnRef()) && hasText(manual.getReturnFailedImage());
        }
        return false;
    }

    private static boolean isCompleteForScenario(TokenScenarioEvidence evidence, QrDirectManualScenario scenario) {
        if (scenario == QrDirectManualScenario.CREATE_PAY_SUCCESS) {
            return hasText(evidence.getRequestLog())
                    && hasText(evidence.getResponseLog())
                    && hasText(evidence.getImage());
        }
        if (scenario == QrDirectManualScenario.QR_SCAN_PAY_SUCCESS
                || scenario == QrDirectManualScenario.QR_SCAN_PAY_FAILED
                || scenario == QrDirectManualScenario.OPEN_APP_SUCCESS
                || scenario == QrDirectManualScenario.OPEN_APP_NOT_INSTALLED
                || scenario == QrDirectManualScenario.OPEN_APP_FAILED) {
            return hasText(evidence.getRequestLog()) && hasText(evidence.getImage());
        }
        return hasText(evidence.getRequestLog()) && hasText(evidence.getResponseLog());
    }

    private static String extractTxnRef(String requestLog) {
        return ManualEvidenceLogParser.extractTxnRef(requestLog);
    }

    private static boolean hasAnyField(TokenScenarioEvidence evidence) {
        return hasText(evidence.getRequestLog())
                || hasText(evidence.getResponseLog())
                || hasText(evidence.getImage());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<QrDirectManualScenario, TokenScenarioEvidence> emptyMap() {
        return new EnumMap<>(QrDirectManualScenario.class);
    }
}
