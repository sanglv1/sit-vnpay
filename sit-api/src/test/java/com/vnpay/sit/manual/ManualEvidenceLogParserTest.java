package com.vnpay.sit.manual;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManualEvidenceLogParserTest {

    @Test
    void parse_shouldExtractGetUrlParams() {
        Map<String, String> params = ManualEvidenceLogParser.parse(
                "GET https://sandbox.vnpayment.vn/token_web/verify? vnp_command=token_create&vnp_txn_ref=TXN001&vnp_tmn_code=TMN01");

        assertThat(params)
                .containsEntry("vnp_command", "token_create")
                .containsEntry("vnp_txn_ref", "TXN001")
                .containsEntry("vnp_tmn_code", "TMN01");
        assertThat(ManualEvidenceLogParser.lookup(params, "vnp_txn_ref")).isEqualTo("TXN001");
    }

    @Test
    void parse_shouldExtractJsonBody() {
        String raw = """
                POST /isp/api/v2/recurring/register
                {
                  "reqId": "REQ001",
                  "command": "register",
                  "order": { "orderReference": "ORD001" },
                  "tmnCode": "TMN02",
                  "transaction": { "recurringAmount": 100000 }
                }
                """;

        Map<String, String> params = ManualEvidenceLogParser.parse(raw);

        assertThat(ManualEvidenceLogParser.lookup(params, "reqId")).isEqualTo("REQ001");
        assertThat(ManualEvidenceLogParser.lookup(params, "command")).isEqualTo("register");
        assertThat(ManualEvidenceLogParser.lookup(params, "order.orderReference")).isEqualTo("ORD001");
        assertThat(ManualEvidenceLogParser.lookup(params, "tmnCode")).isEqualTo("TMN02");
        assertThat(ManualEvidenceLogParser.lookup(params, "transaction.recurringAmount")).isEqualTo("100000");
    }

    @Test
    void parse_shouldExtractLineBasedFields() {
        Map<String, String> params = ManualEvidenceLogParser.parse("""
                clientId: merchant-client
                username: sit-user
                password: secret
                clientSecret: cs-001
                """);

        assertThat(ManualEvidenceLogParser.lookup(params, "clientId")).isEqualTo("merchant-client");
        assertThat(ManualEvidenceLogParser.lookup(params, "username")).isEqualTo("sit-user");
    }

    @Test
    void parse_shouldExtractResponseRspCode() {
        Map<String, String> params = ManualEvidenceLogParser.parse("""
                HTTP/1.1 200 OK
                {"rspCode":"00","data":{"accessToken":"abc","expiresIn":3600}}
                """);

        assertThat(ManualEvidenceLogParser.lookup(params, "rspCode")).isEqualTo("00");
        assertThat(ManualEvidenceLogParser.lookup(params, "accessToken")).isEqualTo("abc");
        assertThat(ManualEvidenceLogParser.formatFieldLine("rspCode", "00")).isEqualTo("rspCode: \"00\"");
    }

    @Test
    void extractTxnRef_shouldResolveFromMixedFormats() {
        assertThat(ManualEvidenceLogParser.extractTxnRef(
                "GET https://pay.vnpay.vn? vnp_txn_ref=ABC123&vnp_command=pay_and_create"))
                .isEqualTo("ABC123");
        assertThat(ManualEvidenceLogParser.extractTxnRef("{\"order\":{\"orderReference\":\"ORD9\"}}"))
                .isEqualTo("ORD9");
    }

    @Test
    void templateFieldLabel_shouldRecognizeOutputExampleLines() {
        assertThat(ManualEvidenceLogParser.templateFieldLabel("rspCode: \"\"")).contains("rspCode");
        assertThat(ManualEvidenceLogParser.templateFieldLabel("rspCode: \"00\"")).contains("rspCode");
        assertThat(ManualEvidenceLogParser.templateFieldLabel("vnp_response_code: 00")).contains("vnp_response_code");
        assertThat(ManualEvidenceLogParser.isTemplateFieldLine("clientId:")).isTrue();
    }

    @Test
    void formatAsFieldLines_shouldNotDuplicateAliases() {
        Map<String, String> params = ManualEvidenceLogParser.parse("""
                {"rspCode":"01","rspMsg":"Authentication failure"}
                """);
        String formatted = ManualEvidenceLogParser.formatAsFieldLines(params);
        assertThat(formatted).contains("rspCode: \"01\"");
        assertThat(formatted).doesNotContain("rsp_code:");
        assertThat(formatted).doesNotContain("RspCode:");
    }
}
