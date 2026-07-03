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

public final class PreAuthManualEvidenceSupport {

    private static final TypeReference<Map<String, TokenScenarioEvidence>> MAP_TYPE = new TypeReference<>() {};

    private PreAuthManualEvidenceSupport() {
    }

    public static Map<PreAuthManualScenario, TokenScenarioEvidence> parse(String json, ObjectMapper objectMapper) {
        Map<PreAuthManualScenario, TokenScenarioEvidence> result = emptyMap();
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
                    PreAuthManualScenario scenario = PreAuthManualScenario.valueOf(entry.getKey());
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

    public static String serialize(Map<PreAuthManualScenario, TokenScenarioEvidence> evidence, ObjectMapper objectMapper) {
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
            throw new IllegalArgumentException("Không lưu được bằng chứng PreAuth", ex);
        }
    }

    public static Map<PreAuthManualScenario, TokenScenarioEvidence> withLegacyCreateToken(
            Map<PreAuthManualScenario, TokenScenarioEvidence> evidence,
            ManualAcceptance entity
    ) {
        Map<PreAuthManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(evidence != null ? evidence : emptyMap());
        mergeLegacy(merged, PreAuthManualScenario.CARD_LINK_VERIFY_SUCCESS,
                entity.getReturnSuccessTxnRef(), entity.getReturnSuccessImage());
        mergeLegacy(merged, PreAuthManualScenario.CARD_LINK_VERIFY_FAILED,
                entity.getReturnFailedTxnRef(), entity.getReturnFailedImage());
        return merged;
    }

    public static void syncLegacyReturnFields(
            ManualAcceptance entity,
            Map<PreAuthManualScenario, TokenScenarioEvidence> evidence
    ) {
        if (evidence == null) {
            return;
        }
        TokenScenarioEvidence createSuccess = evidence.get(PreAuthManualScenario.CARD_LINK_VERIFY_SUCCESS);
        if (createSuccess != null) {
            if (hasText(createSuccess.getImage())) {
                entity.setReturnSuccessImage(createSuccess.getImage());
            }
            if (hasText(createSuccess.getRequestLog()) && !hasText(entity.getReturnSuccessTxnRef())) {
                entity.setReturnSuccessTxnRef(extractTxnRef(createSuccess.getRequestLog()));
            }
        }
        TokenScenarioEvidence createFailed = evidence.get(PreAuthManualScenario.CARD_LINK_VERIFY_FAILED);
        if (createFailed != null) {
            if (hasText(createFailed.getImage())) {
                entity.setReturnFailedImage(createFailed.getImage());
            }
            if (hasText(createFailed.getRequestLog()) && !hasText(entity.getReturnFailedTxnRef())) {
                entity.setReturnFailedTxnRef(extractTxnRef(createFailed.getRequestLog()));
            }
        }
    }

    public static Map<String, TokenScenarioEvidence> toApiMap(
            Map<PreAuthManualScenario, TokenScenarioEvidence> evidence
    ) {
        Map<String, TokenScenarioEvidence> api = new LinkedHashMap<>();
        if (evidence == null) {
            return api;
        }
        for (PreAuthManualScenario scenario : PreAuthManualScenario.values()) {
            TokenScenarioEvidence value = evidence.get(scenario);
            if (value != null && hasAnyField(value)) {
                api.put(scenario.name(), value);
            }
        }
        return api;
    }

    public static Map<PreAuthManualScenario, TokenScenarioEvidence> fromFormMap(
            Map<String, TokenScenarioEvidence> formMap
    ) {
        Map<PreAuthManualScenario, TokenScenarioEvidence> result = emptyMap();
        if (formMap == null) {
            return result;
        }
        for (Map.Entry<String, TokenScenarioEvidence> entry : formMap.entrySet()) {
            try {
                PreAuthManualScenario scenario = PreAuthManualScenario.valueOf(entry.getKey());
                if (entry.getValue() != null) {
                    result.put(scenario, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown keys
            }
        }
        return result;
    }

    public static Map<PreAuthManualScenario, TokenScenarioEvidence> mergeEvidence(
            Map<PreAuthManualScenario, TokenScenarioEvidence> existing,
            Map<PreAuthManualScenario, TokenScenarioEvidence> incoming,
            ManualAcceptance entity
    ) {
        Map<PreAuthManualScenario, TokenScenarioEvidence> merged = new EnumMap<>(PreAuthManualScenario.class);
        for (PreAuthManualScenario scenario : PreAuthManualScenario.values()) {
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
        return withLegacyCreateToken(merged, entity);
    }

    private static void mergeLegacy(
            Map<PreAuthManualScenario, TokenScenarioEvidence> merged,
            PreAuthManualScenario scenario,
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

    public static Optional<TokenScenarioEvidence> evidence(
            ManualAcceptance manual,
            PreAuthManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (manual == null) {
            return Optional.empty();
        }
        Map<PreAuthManualScenario, TokenScenarioEvidence> map =
                withLegacyCreateToken(parse(manual.getPreauthScenarioEvidence(), objectMapper), manual);
        return Optional.ofNullable(map.get(scenario));
    }

    public static boolean passesEvaluation(
            ManualAcceptance manual,
            PreAuthManualScenario scenario,
            ObjectMapper objectMapper
    ) {
        if (evidence(manual, scenario, objectMapper).map(v -> isCompleteForScenario(v, scenario)).orElse(false)) {
            return true;
        }
        if (manual == null) {
            return false;
        }
        if (scenario == PreAuthManualScenario.CARD_LINK_VERIFY_SUCCESS) {
            return hasText(manual.getReturnSuccessTxnRef()) && hasText(manual.getReturnSuccessImage());
        }
        if (scenario == PreAuthManualScenario.CARD_LINK_VERIFY_FAILED) {
            return hasText(manual.getReturnFailedTxnRef()) && hasText(manual.getReturnFailedImage());
        }
        return false;
    }

    private static boolean isCompleteForScenario(TokenScenarioEvidence evidence, PreAuthManualScenario scenario) {
        if (!hasText(evidence.getRequestLog()) || !hasText(evidence.getResponseLog())) {
            return false;
        }
        if (scenario == PreAuthManualScenario.CARD_LINK_VERIFY_SUCCESS
                || scenario == PreAuthManualScenario.CARD_LINK_VERIFY_FAILED
                || scenario == PreAuthManualScenario.PREAUTH_3DS_SUCCESS
                || scenario == PreAuthManualScenario.PREAUTH_3DS_FAILED) {
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

    private static Map<PreAuthManualScenario, TokenScenarioEvidence> emptyMap() {
        return new EnumMap<>(PreAuthManualScenario.class);
    }
}
