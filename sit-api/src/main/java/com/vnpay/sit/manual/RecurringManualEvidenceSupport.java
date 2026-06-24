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

public final class RecurringManualEvidenceSupport {

    private static final TypeReference<Map<String, TokenScenarioEvidence>> MAP_TYPE = new TypeReference<>() {};

    private RecurringManualEvidenceSupport() {
    }

    public static Map<RecurringManualScenario, TokenScenarioEvidence> parse(String json, ObjectMapper objectMapper) {
        Map<RecurringManualScenario, TokenScenarioEvidence> result = emptyMap();
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
                    RecurringManualScenario scenario = RecurringManualScenario.valueOf(entry.getKey());
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
            Map<RecurringManualScenario, TokenScenarioEvidence> evidence,
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
            throw new IllegalArgumentException("Không lưu được bằng chứng Recurring", ex);
        }
    }

    public static Map<RecurringManualScenario, TokenScenarioEvidence> withLegacyCardVerify(
            Map<RecurringManualScenario, TokenScenarioEvidence> evidence,
            ManualAcceptance entity
    ) {
        Map<RecurringManualScenario, TokenScenarioEvidence> merged =
                new EnumMap<>(evidence != null ? evidence : emptyMap());
        mergeLegacy(merged, RecurringManualScenario.CARD_VERIFY_SUCCESS,
                entity.getReturnSuccessTxnRef(), entity.getReturnSuccessImage());
        mergeLegacy(merged, RecurringManualScenario.CARD_VERIFY_FAILED,
                entity.getReturnFailedTxnRef(), entity.getReturnFailedImage());
        return merged;
    }

    public static void syncLegacyReturnFields(
            ManualAcceptance entity,
            Map<RecurringManualScenario, TokenScenarioEvidence> evidence
    ) {
        if (evidence == null) {
            return;
        }
        TokenScenarioEvidence cardSuccess = evidence.get(RecurringManualScenario.CARD_VERIFY_SUCCESS);
        if (cardSuccess != null) {
            if (hasText(cardSuccess.getImage())) {
                entity.setReturnSuccessImage(cardSuccess.getImage());
            }
            if (hasText(cardSuccess.getRequestLog()) && !hasText(entity.getReturnSuccessTxnRef())) {
                entity.setReturnSuccessTxnRef(extractTxnRef(cardSuccess.getRequestLog()));
            }
        }
        TokenScenarioEvidence cardFailed = evidence.get(RecurringManualScenario.CARD_VERIFY_FAILED);
        if (cardFailed != null) {
            if (hasText(cardFailed.getImage())) {
                entity.setReturnFailedImage(cardFailed.getImage());
            }
            if (hasText(cardFailed.getRequestLog()) && !hasText(entity.getReturnFailedTxnRef())) {
                entity.setReturnFailedTxnRef(extractTxnRef(cardFailed.getRequestLog()));
            }
        }
    }

    public static Optional<TokenScenarioEvidence> evidence(
            ManualAcceptance manual,
            RecurringManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (manual == null) {
            return Optional.empty();
        }
        Map<RecurringManualScenario, TokenScenarioEvidence> map =
                withLegacyCardVerify(parse(manual.getRecurringScenarioEvidence(), objectMapper), manual);
        return Optional.ofNullable(map.get(scenario));
    }

    public static boolean passesEvaluation(
            ManualAcceptance manual,
            RecurringManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (evidence(manual, scenario, objectMapper).map(value -> isCompleteForScenario(value, scenario)).orElse(false)) {
            return true;
        }
        if (manual == null) {
            return false;
        }
        if (scenario == RecurringManualScenario.CARD_VERIFY_SUCCESS) {
            return hasText(manual.getReturnSuccessTxnRef()) && hasText(manual.getReturnSuccessImage());
        }
        if (scenario == RecurringManualScenario.CARD_VERIFY_FAILED) {
            return hasText(manual.getReturnFailedTxnRef()) && hasText(manual.getReturnFailedImage());
        }
        return false;
    }

    public static Map<String, TokenScenarioEvidence> toApiMap(
            Map<RecurringManualScenario, TokenScenarioEvidence> evidence
    ) {
        Map<String, TokenScenarioEvidence> api = new LinkedHashMap<>();
        if (evidence == null) {
            return api;
        }
        for (RecurringManualScenario scenario : RecurringManualScenario.values()) {
            TokenScenarioEvidence value = evidence.get(scenario);
            if (value != null && hasAnyField(value)) {
                api.put(scenario.name(), value);
            }
        }
        return api;
    }

    public static Map<RecurringManualScenario, TokenScenarioEvidence> fromFormMap(
            Map<String, TokenScenarioEvidence> formMap
    ) {
        Map<RecurringManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (formMap == null) {
            return result;
        }
        for (Map.Entry<String, TokenScenarioEvidence> entry : formMap.entrySet()) {
            try {
                RecurringManualScenario scenario = RecurringManualScenario.valueOf(entry.getKey());
                if (entry.getValue() != null) {
                    result.put(scenario, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown keys
            }
        }
        return result;
    }

    public static Map<RecurringManualScenario, TokenScenarioEvidence> mergeEvidence(
            Map<RecurringManualScenario, TokenScenarioEvidence> existing,
            Map<RecurringManualScenario, TokenScenarioEvidence> incoming,
            ManualAcceptance entity
    ) {
        Map<RecurringManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(RecurringManualScenario.class);
        for (RecurringManualScenario scenario : RecurringManualScenario.values()) {
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
        return withLegacyCardVerify(merged, entity);
    }

    private static void mergeLegacy(
            Map<RecurringManualScenario, TokenScenarioEvidence> merged,
            RecurringManualScenario scenario,
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

    private static boolean isCompleteForScenario(TokenScenarioEvidence evidence, RecurringManualScenario scenario) {
        if (!hasText(evidence.getRequestLog()) || !hasText(evidence.getResponseLog())) {
            return false;
        }
        if (scenario == RecurringManualScenario.CARD_VERIFY_SUCCESS
                || scenario == RecurringManualScenario.CARD_VERIFY_FAILED) {
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

    private static Map<RecurringManualScenario, TokenScenarioEvidence> emptyMap() {
        return new EnumMap<>(RecurringManualScenario.class);
    }
}
