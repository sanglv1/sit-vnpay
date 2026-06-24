package com.vnpay.sit.manual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses QC manual evidence logs (HTTP, JSON, form body, key:value lines) into template field values.
 */
public final class ManualEvidenceLogParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern URL_QUERY = Pattern.compile("[?&]([^=&?#\\s]+)=([^&?#\\s]*)");

    private ManualEvidenceLogParser() {
    }

    public static Map<String, String> parse(String raw) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return params;
        }
        String text = raw.trim();
        parseJsonBlocks(text, params);
        parseUrls(text, params);
        parseFormBody(text, params);
        parseLines(text, params);
        addAliases(params);
        return params;
    }

    public static String lookup(Map<String, String> params, String label) {
        if (params == null || params.isEmpty() || label == null || label.isBlank()) {
            return null;
        }
        String normalized = normalizeLabel(label);
        if (params.containsKey(normalized)) {
            return params.get(normalized);
        }
        for (String alias : aliasesFor(normalized)) {
            if (params.containsKey(alias)) {
                return params.get(alias);
            }
        }
        return null;
    }

    public static String formatFieldLine(String label, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = normalizeLabel(label);
        if ("rspcode".equalsIgnoreCase(normalized) || "rspCode".equals(normalized)) {
            return normalized + ": " + quote(value.trim());
        }
        return normalized + ": " + value.trim();
    }

    public static String formatAsFieldLines(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        Set<String> written = new LinkedHashSet<>();
        for (String key : preferredOrder()) {
            String value = lookup(params, key);
            if (value != null && !value.isBlank() && written.add(key)) {
                String line = formatFieldLine(key, value);
                if (line != null) {
                    lines.add(line);
                }
            }
        }
        return String.join("\n", lines);
    }

    public static Optional<String> templateFieldLabel(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("Input:") || trimmed.equalsIgnoreCase("Output:")) {
            return Optional.empty();
        }
        if (trimmed.equals("..") || trimmed.startsWith("...")) {
            return Optional.empty();
        }
        int colon = trimmed.indexOf(':');
        if (colon <= 0) {
            return Optional.empty();
        }
        String label = trimmed.substring(0, colon).trim();
        if (label.isEmpty() || label.startsWith("Ma ")) {
            return Optional.empty();
        }
        String normalized = normalizeLabel(label);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        boolean emptyPlaceholder = colon == trimmed.length() - 1;
        if (emptyPlaceholder || isOutputExampleField(normalized)) {
            return Optional.of(normalized);
        }
        return Optional.empty();
    }

    public static boolean isTemplateFieldLine(String text) {
        return templateFieldLabel(text).isPresent();
    }

    private static boolean isOutputExampleField(String label) {
        return "rspCode".equalsIgnoreCase(label)
                || label.startsWith("vnp_")
                || "Message".equalsIgnoreCase(label);
    }

    public static String extractTxnRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Map<String, String> params = parse(raw);
        String ref = lookup(params, "vnp_txn_ref");
        if (ref != null && !ref.isBlank()) {
            return ref.trim();
        }
        ref = lookup(params, "order.orderReference");
        if (ref != null && !ref.isBlank()) {
            return ref.trim();
        }
        return null;
    }

    private static void parseJsonBlocks(String text, Map<String, String> params) {
        try {
            JsonNode root = MAPPER.readTree(text);
            flattenJson("", root, params);
            return;
        } catch (Exception ignored) {
            // not a single JSON document
        }
        int index = 0;
        while (index < text.length()) {
            int start = text.indexOf('{', index);
            if (start < 0) {
                break;
            }
            int end = findMatchingBrace(text, start);
            if (end < 0) {
                break;
            }
            String block = text.substring(start, end + 1);
            try {
                JsonNode node = MAPPER.readTree(block);
                flattenJson("", node, params);
            } catch (Exception ignored) {
                // skip invalid block
            }
            index = end + 1;
        }
    }

    private static int findMatchingBrace(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void flattenJson(String prefix, JsonNode node, Map<String, String> params) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenJson(key, entry.getValue(), params);
            });
            return;
        }
        if (node.isArray()) {
            return;
        }
        String value = node.asText("");
        if (!value.isBlank()) {
            put(params, prefix, value);
            put(params, toSnakeCase(prefix), value);
            int dot = prefix.lastIndexOf('.');
            if (dot >= 0 && dot < prefix.length() - 1) {
                put(params, prefix.substring(dot + 1), value);
            }
        }
    }

    private static void parseUrls(String text, Map<String, String> params) {
        String normalized = text.replace("? ", "?").replace("& ", "&");
        Matcher matcher = URL_QUERY.matcher(normalized);
        while (matcher.find()) {
            put(params, decode(matcher.group(1)), decode(matcher.group(2)));
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("GET ") && !trimmed.startsWith("POST ")) {
                continue;
            }
            int queryStart = trimmed.indexOf('?');
            if (queryStart < 0) {
                continue;
            }
            parseQueryString(trimmed.substring(queryStart + 1), params);
        }
    }

    private static void parseQueryString(String query, Map<String, String> params) {
        String normalized = query.replace(" ", "");
        for (String pair : normalized.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            put(params, decode(pair.substring(0, eq).trim()), decode(pair.substring(eq + 1).trim()));
        }
    }

    private static void parseFormBody(String text, Map<String, String> params) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.contains("=") || trimmed.contains("://")) {
                continue;
            }
            if (trimmed.startsWith("GET ") || trimmed.startsWith("POST ")) {
                continue;
            }
            String[] pairs = trimmed.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                put(params, decode(pair.substring(0, eq).trim()), decode(pair.substring(eq + 1).trim()));
            }
        }
    }

    private static void parseLines(String text, Map<String, String> params) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("GET ") || trimmed.startsWith("POST ")) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String key = trimmed.substring(0, colon).trim();
                String value = trimmed.substring(colon + 1).trim();
                if (!key.isEmpty() && !value.isEmpty() && !key.equalsIgnoreCase("http") && !key.equalsIgnoreCase("https")) {
                    put(params, key, unquote(value));
                }
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                put(params, trimmed.substring(0, eq).trim(), unquote(trimmed.substring(eq + 1).trim()));
            }
        }
    }

    private static void addAliases(Map<String, String> params) {
        alias(params, "vnp_txn_ref", "order.orderReference", "orderReference", "TxnRef", "txnRef");
        alias(params, "vnp_app_user_id", "app.userId", "userId");
        alias(params, "vnp_tmn_code", "tmnCode", "TmnCode");
        alias(params, "vnp_amount", "transaction.recurringAmount", "recurringAmount", "amount");
        alias(params, "vnp_token", "token.tokenId", "tokenId");
        alias(params, "vnp_command", "command");
        alias(params, "vnp_response_code", "responseCode", "response_code");
        alias(params, "vnp_transaction_status", "transactionStatus", "transaction_status");
        alias(params, "rspCode", "RspCode", "rsp_code");
        alias(params, "transaction.recurringId", "recurringId");
        alias(params, "transaction.recurringNumber", "recurringNumber");
        alias(params, "transaction.amount", "transactionAmount");
    }

    private static void alias(Map<String, String> params, String canonical, String... aliases) {
        String value = params.get(canonical);
        if (value == null || value.isBlank()) {
            for (String alias : aliases) {
                value = params.get(alias);
                if (value != null && !value.isBlank()) {
                    break;
                }
            }
        }
        if (value == null || value.isBlank()) {
            return;
        }
        put(params, canonical, value);
        for (String alias : aliases) {
            put(params, alias, value);
        }
    }

    private static List<String> preferredOrder() {
        return List.of(
                "clientId", "username", "password", "clientSecret",
                "accessToken", "reqId", "command", "order.orderReference", "tmnCode",
                "amount", "currCode", "secureHash", "transaction.mcDate",
                "transaction.recurringAmount", "app.userId", "ispTxnId", "dataKey",
                "transaction.recurringId", "token.tokenId", "transaction.recurringNumber", "transaction.amount",
                "vnp_tmn_code", "vnp_token", "vnp_command", "vnp_txn_ref",
                "vnp_response_code", "vnp_transaction_status", "rspCode", "rspMsg"
        );
    }

    private static Set<String> aliasesFor(String label) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        switch (normalizeLabel(label)) {
            case "clientId" -> aliases.add("client_id");
            case "accessToken" -> aliases.add("access_token");
            case "clientSecret" -> aliases.add("client_secret");
            case "reqId" -> {
                aliases.add("req_id");
                aliases.add("requestId");
            }
            case "command" -> aliases.add("vnp_command");
            case "order.orderReference" -> {
                aliases.add("orderReference");
                aliases.add("vnp_txn_ref");
                aliases.add("txnRef");
            }
            case "tmnCode" -> {
                aliases.add("vnp_tmn_code");
                aliases.add("vnp_TmnCode");
            }
            case "transaction.recurringAmount" -> {
                aliases.add("recurringAmount");
                aliases.add("vnp_amount");
            }
            case "app.userId" -> {
                aliases.add("userId");
                aliases.add("vnp_app_user_id");
            }
            case "ispTxnId" -> aliases.add("isp_txn_id");
            case "dataKey" -> aliases.add("data_key");
            case "transaction.recurringId" -> aliases.add("recurringId");
            case "token.tokenId" -> {
                aliases.add("tokenId");
                aliases.add("vnp_token");
            }
            case "transaction.recurringNumber" -> aliases.add("recurringNumber");
            case "transaction.amount" -> aliases.add("transactionAmount");
            case "vnp_txn_ref" -> {
                aliases.add("order.orderReference");
                aliases.add("orderReference");
            }
            case "rspCode" -> aliases.add("RspCode");
            case "vnp_response_code" -> aliases.add("responseCode");
            case "vnp_transaction_status" -> aliases.add("transactionStatus");
            default -> {
                aliases.add(toSnakeCase(label));
                aliases.add(toCamelCase(label));
            }
        }
        return aliases;
    }

    private static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        int paren = trimmed.indexOf('(');
        if (paren > 0) {
            trimmed = trimmed.substring(0, paren).trim();
        }
        return trimmed.replaceAll("[^\\w.]", "");
    }

    private static void put(Map<String, String> params, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }
        params.putIfAbsent(key, value);
        String snake = toSnakeCase(key);
        if (!snake.equals(key)) {
            params.putIfAbsent(snake, value);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static String toSnakeCase(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.contains(".")) {
            return key;
        }
        String snake = key
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
        if (snake.startsWith("vnp") && !snake.startsWith("vnp_")) {
            return "vnp_" + snake.substring(3);
        }
        return snake;
    }

    private static String toCamelCase(String key) {
        if (key == null || key.isBlank() || !key.contains("_")) {
            return key;
        }
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
