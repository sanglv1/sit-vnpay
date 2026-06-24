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

public final class InstalmentManualEvidenceSupport {

    private static final TypeReference<Map<String, TokenScenarioEvidence>> MAP_TYPE = new TypeReference<>() {};

    private InstalmentManualEvidenceSupport() {
    }

    public static Map<InstalmentManualScenario, TokenScenarioEvidence> parse(String json, ObjectMapper objectMapper) {
        Map<InstalmentManualScenario, TokenScenarioEvidence> result = emptyMap();
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
                    InstalmentManualScenario scenario = InstalmentManualScenario.valueOf(entry.getKey());
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

    public static String serialize(
            Map<InstalmentManualScenario, TokenScenarioEvidence> evidence,
            ObjectMapper objectMapper
    ) {
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
            throw new IllegalArgumentException("Không lưu được bằng chứng Instalment", ex);
        }
    }

    public static Map<InstalmentManualScenario, TokenScenarioEvidence> withLegacyPayReturn(
            Map<InstalmentManualScenario, TokenScenarioEvidence> evidence,
            ManualAcceptance entity
    ) {
        Map<InstalmentManualScenario, TokenScenarioEvidence> merged =
                new EnumMap<>(evidence != null ? evidence : emptyMap());
        mergeLegacy(merged, InstalmentManualScenario.PAY_SUCCESS,
                entity.getReturnSuccessTxnRef(), entity.getReturnSuccessImage());
        mergeLegacy(merged, InstalmentManualScenario.PAY_FAILED,
                entity.getReturnFailedTxnRef(), entity.getReturnFailedImage());
        return merged;
    }

    public static void syncLegacyReturnFields(
            ManualAcceptance entity,
            Map<InstalmentManualScenario, TokenScenarioEvidence> evidence
    ) {
        if (evidence == null) {
            return;
        }
        TokenScenarioEvidence paySuccess = evidence.get(InstalmentManualScenario.PAY_SUCCESS);
        if (paySuccess != null) {
            if (hasText(paySuccess.getImage())) {
                entity.setReturnSuccessImage(paySuccess.getImage());
            }
            if (hasText(paySuccess.getRequestLog()) && !hasText(entity.getReturnSuccessTxnRef())) {
                entity.setReturnSuccessTxnRef(extractTxnRef(paySuccess.getRequestLog()));
            }
        }
        TokenScenarioEvidence payFailed = evidence.get(InstalmentManualScenario.PAY_FAILED);
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
            InstalmentManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (manual == null) {
            return Optional.empty();
        }
        Map<InstalmentManualScenario, TokenScenarioEvidence> map =
                withLegacyPayReturn(parse(manual.getInstalmentScenarioEvidence(), objectMapper), manual);
        return Optional.ofNullable(map.get(scenario));
    }

    public static boolean passesEvaluation(
            ManualAcceptance manual,
            InstalmentManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (evidence(manual, scenario, objectMapper).map(value -> isCompleteForScenario(value, scenario)).orElse(false)) {
            return true;
        }
        if (manual == null) {
            return false;
        }
        if (scenario == InstalmentManualScenario.PAY_SUCCESS) {
            return hasText(manual.getReturnSuccessTxnRef()) && hasText(manual.getReturnSuccessImage());
        }
        if (scenario == InstalmentManualScenario.PAY_FAILED) {
            return hasText(manual.getReturnFailedTxnRef()) && hasText(manual.getReturnFailedImage());
        }
        return false;
    }

    public static Map<String, TokenScenarioEvidence> toApiMap(
            Map<InstalmentManualScenario, TokenScenarioEvidence> evidence
    ) {
        Map<String, TokenScenarioEvidence> api = new LinkedHashMap<>();
        if (evidence == null) {
            return api;
        }
        for (InstalmentManualScenario scenario : InstalmentManualScenario.values()) {
            TokenScenarioEvidence value = evidence.get(scenario);
            if (value != null && hasAnyField(value)) {
                api.put(scenario.name(), value);
            }
        }
        return api;
    }

    public static Map<InstalmentManualScenario, TokenScenarioEvidence> fromFormMap(
            Map<String, TokenScenarioEvidence> formMap
    ) {
        Map<InstalmentManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (formMap == null) {
            return result;
        }
        for (Map.Entry<String, TokenScenarioEvidence> entry : formMap.entrySet()) {
            try {
                InstalmentManualScenario scenario = InstalmentManualScenario.valueOf(entry.getKey());
                if (entry.getValue() != null) {
                    result.put(scenario, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown keys
            }
        }
        return result;
    }

    public static Map<InstalmentManualScenario, TokenScenarioEvidence> mergeEvidence(
            Map<InstalmentManualScenario, TokenScenarioEvidence> existing,
            Map<InstalmentManualScenario, TokenScenarioEvidence> incoming,
            ManualAcceptance entity
    ) {
        Map<InstalmentManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(InstalmentManualScenario.class);
        for (InstalmentManualScenario scenario : InstalmentManualScenario.values()) {
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
        return withLegacyPayReturn(merged, entity);
    }

    private static void mergeLegacy(
            Map<InstalmentManualScenario, TokenScenarioEvidence> merged,
            InstalmentManualScenario scenario,
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

    private static boolean isCompleteForScenario(TokenScenarioEvidence evidence, InstalmentManualScenario scenario) {
        if (!hasText(evidence.getRequestLog()) || !hasText(evidence.getResponseLog())) {
            return false;
        }
        if (scenario == InstalmentManualScenario.QUERY_CONFIG_SUCCESS
                || scenario == InstalmentManualScenario.PAY_SUCCESS
                || scenario == InstalmentManualScenario.PAY_FAILED) {
            return hasText(evidence.getImage());
        }
        return true;
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

    private static Map<InstalmentManualScenario, TokenScenarioEvidence> emptyMap() {
        return new EnumMap<>(InstalmentManualScenario.class);
    }
}
