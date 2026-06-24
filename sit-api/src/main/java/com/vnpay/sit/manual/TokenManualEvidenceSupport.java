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

public final class TokenManualEvidenceSupport {

    private static final TypeReference<Map<String, TokenScenarioEvidence>> MAP_TYPE = new TypeReference<>() {};

    private TokenManualEvidenceSupport() {
    }

    public static Map<TokenManualScenario, TokenScenarioEvidence> parse(String json, ObjectMapper objectMapper) {
        Map<TokenManualScenario, TokenScenarioEvidence> result = emptyMap();
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
                    TokenManualScenario scenario = TokenManualScenario.valueOf(entry.getKey());
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

    public static String serialize(Map<TokenManualScenario, TokenScenarioEvidence> evidence, ObjectMapper objectMapper) {
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
            throw new IllegalArgumentException("Không lưu được bằng chứng Token", ex);
        }
    }

    public static Map<TokenManualScenario, TokenScenarioEvidence> withLegacyPayAndCreate(
            Map<TokenManualScenario, TokenScenarioEvidence> evidence,
            ManualAcceptance entity
    ) {
        Map<TokenManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(evidence != null ? evidence : emptyMap());
        mergeLegacy(merged, TokenManualScenario.PAY_AND_CREATE_SUCCESS,
                entity.getReturnSuccessTxnRef(), entity.getReturnSuccessImage());
        mergeLegacy(merged, TokenManualScenario.PAY_AND_CREATE_FAILED,
                entity.getReturnFailedTxnRef(), entity.getReturnFailedImage());
        return merged;
    }

    public static void syncLegacyReturnFields(
            ManualAcceptance entity,
            Map<TokenManualScenario, TokenScenarioEvidence> evidence
    ) {
        if (evidence == null) {
            return;
        }
        TokenScenarioEvidence paySuccess = evidence.get(TokenManualScenario.PAY_AND_CREATE_SUCCESS);
        if (paySuccess != null) {
            if (hasText(paySuccess.getImage())) {
                entity.setReturnSuccessImage(paySuccess.getImage());
            }
            if (hasText(paySuccess.getRequestLog()) && !hasText(entity.getReturnSuccessTxnRef())) {
                entity.setReturnSuccessTxnRef(extractTxnRef(paySuccess.getRequestLog()));
            }
        }
        TokenScenarioEvidence payFailed = evidence.get(TokenManualScenario.PAY_AND_CREATE_FAILED);
        if (payFailed != null) {
            if (hasText(payFailed.getImage())) {
                entity.setReturnFailedImage(payFailed.getImage());
            }
            if (hasText(payFailed.getRequestLog()) && !hasText(entity.getReturnFailedTxnRef())) {
                entity.setReturnFailedTxnRef(extractTxnRef(payFailed.getRequestLog()));
            }
        }
    }

    public static Optional<TokenScenarioEvidence> evidence(
            ManualAcceptance manual,
            TokenManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (manual == null) {
            return Optional.empty();
        }
        Map<TokenManualScenario, TokenScenarioEvidence> map =
                withLegacyPayAndCreate(parse(manual.getTokenScenarioEvidence(), objectMapper), manual);
        return Optional.ofNullable(map.get(scenario));
    }

    public static boolean isScenarioComplete(
            ManualAcceptance manual,
            TokenManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        return evidence(manual, scenario, objectMapper)
                .map(TokenScenarioEvidence::isComplete)
                .orElse(false);
    }

    /** Đánh giá Đạt: đủ 3 phần bằng chứng; Pay&Create vẫn chấp nhận txnRef + ảnh (tương thích cũ). */
    public static boolean passesEvaluation(
            ManualAcceptance manual,
            TokenManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (isScenarioComplete(manual, scenario, objectMapper)) {
            return true;
        }
        if (manual == null) {
            return false;
        }
        if (scenario == TokenManualScenario.PAY_AND_CREATE_SUCCESS) {
            return hasText(manual.getReturnSuccessTxnRef()) && hasText(manual.getReturnSuccessImage());
        }
        if (scenario == TokenManualScenario.PAY_AND_CREATE_FAILED) {
            return hasText(manual.getReturnFailedTxnRef()) && hasText(manual.getReturnFailedImage());
        }
        return false;
    }

    public static Map<String, TokenScenarioEvidence> toApiMap(
            Map<TokenManualScenario, TokenScenarioEvidence> evidence
    ) {
        Map<String, TokenScenarioEvidence> api = new LinkedHashMap<>();
        if (evidence == null) {
            return api;
        }
        for (TokenManualScenario scenario : TokenManualScenario.values()) {
            TokenScenarioEvidence value = evidence.get(scenario);
            if (value != null && hasAnyField(value)) {
                api.put(scenario.name(), value);
            }
        }
        return api;
    }

    public static Map<TokenManualScenario, TokenScenarioEvidence> fromFormMap(
            Map<String, TokenScenarioEvidence> formMap
    ) {
        Map<TokenManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (formMap == null) {
            return result;
        }
        for (Map.Entry<String, TokenScenarioEvidence> entry : formMap.entrySet()) {
            try {
                TokenManualScenario scenario = TokenManualScenario.valueOf(entry.getKey());
                if (entry.getValue() != null) {
                    result.put(scenario, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown keys
            }
        }
        return result;
    }

    public static Map<TokenManualScenario, TokenScenarioEvidence> mergeEvidence(
            Map<TokenManualScenario, TokenScenarioEvidence> existing,
            Map<TokenManualScenario, TokenScenarioEvidence> incoming,
            ManualAcceptance entity
    ) {
        Map<TokenManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(TokenManualScenario.class);
        for (TokenManualScenario scenario : TokenManualScenario.values()) {
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
        return withLegacyPayAndCreate(merged, entity);
    }

    private static void mergeLegacy(
            Map<TokenManualScenario, TokenScenarioEvidence> merged,
            TokenManualScenario scenario,
            String txnRef,
            String image
    ) {
        TokenScenarioEvidence existing = merged.computeIfAbsent(scenario, ignored -> new TokenScenarioEvidence());
        if (!hasText(existing.getImage()) && hasText(image)) {
            existing.setImage(image);
        }
        if (!hasText(existing.getRequestLog()) && hasText(txnRef)) {
            existing.setRequestLog("vnp_txn_ref: " + txnRef.trim());
        }
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

    private static Map<TokenManualScenario, TokenScenarioEvidence> emptyMap() {
        return new EnumMap<>(TokenManualScenario.class);
    }
}
