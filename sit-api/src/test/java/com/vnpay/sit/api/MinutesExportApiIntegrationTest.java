package com.vnpay.sit.api;

import com.vnpay.sit.export.DocxLayoutPolisher;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.repository.ManualAcceptanceRepository;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MinutesExportApiIntegrationTest {

    private static final String TINY_PNG_DATA_URL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PartnerConfigRepository partnerConfigRepository;
    @Autowired
    private TestSessionRepository testSessionRepository;
    @Autowired
    private TestRunRepository testRunRepository;
    @Autowired
    private ManualAcceptanceRepository manualAcceptanceRepository;

    @MockBean
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        doNothing().when(accessControlService).requireSessionAccess(anyLong(), any());
        testRunRepository.deleteAll();
        manualAcceptanceRepository.deleteAll();
        testSessionRepository.deleteAll();
        partnerConfigRepository.deleteAll();
    }

    @Test
    void exportMinutes_shouldRenderImportantFieldsIntoDocx() throws Exception {
        PartnerConfig partner = new PartnerConfig();
        partner.setName("Merchant A");
        partner.setFlow(PaymentFlow.PAY);
        partner.setTmnCode("TMN999");
        partner.setSecretKey("secret");
        partner.setReturnUrl("https://merchant.test/return");
        partner.setIpnUrl("https://merchant.test/ipn");
        partner.setCreatedByEmail("owner@merchant.test");
        partner = partnerConfigRepository.save(partner);

        TestSession session = new TestSession();
        session.setPartnerId(partner.getId());
        session.setPartnerName(partner.getName());
        session.setTmnCode(partner.getTmnCode());
        session.setCreatedByEmail("qc@merchant.test");
        session.setStatus("OPEN");
        session = testSessionRepository.save(session);

        saveRun(session, partner, TestCaseType.SUCCESS, true, "CASE1");
        saveRun(session, partner, TestCaseType.ORDER_ALREADY_CONFIRMED, true, "CASE2");
        saveRun(session, partner, TestCaseType.FAILED, false, "CASE3");
        saveRun(session, partner, TestCaseType.ORDER_NOT_FOUND, true, "CASE4");
        saveRun(session, partner, TestCaseType.WRONG_AMOUNT, false, "CASE5");
        saveRun(session, partner, TestCaseType.INVALID_HASH, true, "CASE6");

        ManualAcceptance manual = new ManualAcceptance();
        manual.setPartnerId(partner.getId());
        manual.setSessionId(session.getId());
        manual.setPartnerName(partner.getName());
        manual.setReturnSuccessTxnRef("RETURN_OK");
        manual.setReturnFailedTxnRef("RETURN_FAIL");
        manual.setExceptionHandled(true);
        manual.setWhitelistIpPassed(true);
        manual.setLogStoragePassed(false);
        manualAcceptanceRepository.save(manual);

        byte[] content = mockMvc.perform(get("/api/sessions/{id}/export-minutes", session.getId())
                        .param("vnpayRepresentative", "VNPAY QA")
                        .param("integrationVersion", "2.1.0"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String allText = readDocText(content);
        assertThat(allText).contains("TMN999");
        assertThat(allText).contains("qc@merchant.test");
        assertThat(allText).contains("CASE1");
        assertThat(allText).contains("CASE2");
        assertThat(allText).contains("CASE3");
        assertThat(allText).contains("CASE4");
        assertThat(allText).contains("CASE5");
        assertThat(allText).contains("CASE6");
        assertThat(allText).contains("Đã xử lý");
        assertThat(allText).doesNotContain("${TMN_CODE}");
        assertThat(allText).doesNotContain("${CASE_1_RESULT}");
    }

    @Test
    void exportMinutes_instalment_shouldRenderIpnFieldsIntoDocx() throws Exception {
        PartnerConfig partner = new PartnerConfig();
        partner.setName("Merchant Instalment");
        partner.setFlow(PaymentFlow.INSTALMENT);
        partner.setTmnCode("TMNINS");
        partner.setSecretKey("secret");
        partner.setReturnUrl("https://merchant.test/return");
        partner.setIpnUrl("https://merchant.test/ipn");
        partner.setCreatedByEmail("owner@merchant.test");
        partner = partnerConfigRepository.save(partner);

        TestSession session = new TestSession();
        session.setPartnerId(partner.getId());
        session.setPartnerName(partner.getName());
        session.setTmnCode(partner.getTmnCode());
        session.setCreatedByEmail("qc@merchant.test");
        session.setStatus("OPEN");
        session = testSessionRepository.save(session);

        saveInstalmentRun(session, partner, TestCaseType.SUCCESS, true, "INS_OK", "00",
                "{\"vnp_TxnRef\":\"INS_OK\",\"vnp_TmnCode\":\"TMNINS\",\"vnp_Amount\":\"10000000\","
                        + "\"vnp_ResponseCode\":\"00\",\"vnp_TransactionStatus\":\"00\"}",
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveInstalmentRun(session, partner, TestCaseType.FAILED, false, "INS_FAIL", "00",
                "{\"vnp_TxnRef\":\"INS_FAIL\",\"vnp_TmnCode\":\"TMNINS\",\"vnp_ResponseCode\":\"24\","
                        + "\"vnp_TransactionStatus\":\"02\"}",
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveInstalmentRun(session, partner, TestCaseType.ORDER_NOT_FOUND, true, "INS_MISSING", "01",
                "{\"vnp_TxnRef\":\"INS_MISSING\",\"vnp_TmnCode\":\"TMNINS\"}",
                "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
        saveInstalmentRun(session, partner, TestCaseType.ORDER_ALREADY_CONFIRMED, true, "INS_OK", "02",
                "{\"vnp_TxnRef\":\"INS_OK\",\"vnp_TmnCode\":\"TMNINS\"}",
                "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
        saveInstalmentRun(session, partner, TestCaseType.WRONG_AMOUNT, false, "INS_OK", "04",
                "{\"vnp_TxnRef\":\"INS_OK\",\"vnp_TmnCode\":\"TMNINS\",\"vnp_Amount\":\"999\"}",
                "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}");
        saveInstalmentRun(session, partner, TestCaseType.INVALID_HASH, true, "INS_OK", "97",
                "{\"vnp_TxnRef\":\"INS_OK\",\"vnp_TmnCode\":\"TMNINS\",\"vnp_SecureHash\":\"bad\"}",
                "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}");

        ManualAcceptance manual = new ManualAcceptance();
        manual.setPartnerId(partner.getId());
        manual.setSessionId(session.getId());
        manual.setPartnerName(partner.getName());
        manual.setWhitelistIpPassed(true);
        manual.setLogStoragePassed(true);
        manual.setExceptionHandled(true);
        manual.setReturnSuccessTxnRef("INS_OK");
        manual.setReturnFailedTxnRef("INS_FAIL");
        manual.setReturnSuccessImage(TINY_PNG_DATA_URL);
        manual.setReturnFailedImage(TINY_PNG_DATA_URL);
        manualAcceptanceRepository.save(manual);

        byte[] content = mockMvc.perform(get("/api/sessions/{id}/export-minutes", session.getId())
                        .param("vnpayRepresentative", "VNPAY QA")
                        .param("websiteName", "Shop Instalment")
                        .param("integrationVersion", "2.1.1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String allText = readDocText(content);
        assertThat(allText).contains("TMNINS");
        assertThat(allText).contains("Merchant Instalment");
        assertThat(allText).contains("Tên website/app kết nối:");
        assertThat(allText).doesNotContain("Shop Instalment");
        assertThat(allText).contains("IPN URL: https://merchant.test/ipn");
        assertThat(allText).contains("Phiên bản tích hợp: 2.1.1");
        assertThat(allText).contains("INS_OK");
        assertThat(allText).contains("rspCode: \"00\"");
        assertThat(allText).contains("rspCode: \"01\"");
        assertThat(allText).contains("rspCode: \"97\"");
        assertThat(allText).contains("Message: \"Confirm successful\"");
        assertThat(allText).contains("Message: \"Order not found\"");
        assertThat(allText).contains("Đạt");
        assertThat(allText).contains("Không đạt");
        assertThat(allText).contains("Đã xử lý");
        assertThat(allText).contains("Màn hình thông báo:");

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            assertThat(doc.getAllPictures()).isNotEmpty();
            assertThat(DocxLayoutPolisher.countMerchantHeaderBlankLines(doc)).isZero();
        }
    }

    @Test
    void exportMinutes_token_shouldRenderIpnFieldsIntoDocx() throws Exception {
        PartnerConfig partner = new PartnerConfig();
        partner.setName("Merchant Token");
        partner.setFlow(PaymentFlow.TOKEN);
        partner.setTmnCode("TMNTOK");
        partner.setSecretKey("secret");
        partner.setReturnUrl("https://merchant.test/return");
        partner.setIpnUrl("https://merchant.test/ipn");
        partner.setCreatedByEmail("owner@merchant.test");
        partner = partnerConfigRepository.save(partner);

        TestSession session = createSession(partner);
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.SUCCESS, true, "TOK_OK", "00",
                snakeCaseParams("TOK_OK", "TMNTOK", "00", "00"),
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.FAILED, false, "TOK_FAIL", "00",
                snakeCaseParams("TOK_FAIL", "TMNTOK", "24", "02"),
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.ORDER_NOT_FOUND, true, "TOK_MISSING", "01",
                snakeCaseParams("TOK_MISSING", "TMNTOK", null, null),
                "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.ORDER_ALREADY_CONFIRMED, true, "TOK_OK", "02",
                snakeCaseParams("TOK_OK", "TMNTOK", null, null),
                "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.WRONG_AMOUNT, false, "TOK_OK", "04",
                snakeCaseParams("TOK_OK", "TMNTOK", null, null, "999"),
                "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.TOKEN, TestCaseType.INVALID_HASH, true, "TOK_OK", "97",
                snakeCaseParams("TOK_OK", "TMNTOK", null, null),
                "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}");
        saveManualAcceptance(session, partner, true, false, true);

        byte[] content = exportMinutes(session, "2.1.0");
        String allText = readDocText(content);

        assertThat(allText).contains("TMNTOK");
        assertThat(allText).contains("Merchant Token");
        assertThat(allText).contains("IPN URL: https://merchant.test/ipn");
        assertThat(allText).contains("Phiên bản tích hợp: 2.1.0");
        assertThat(allText).contains("TOK_OK");
        assertThat(allText).contains("vnp_txn_ref: TOK_OK");
        assertThat(allText).contains("rspCode: \"00\"");
        assertThat(allText).contains("rspCode: \"01\"");
        assertThat(allText).contains("Message: \"Confirm successful\"");
        assertThat(allText).contains("Đạt");
        assertThat(allText).contains("Không đạt");
        assertThat(allText).contains("Đã xử lý");
    }

    @Test
    void exportMinutes_recurring_shouldRenderIpnFieldsIntoDocx() throws Exception {
        PartnerConfig partner = new PartnerConfig();
        partner.setName("Merchant Recurring");
        partner.setFlow(PaymentFlow.RECURRING);
        partner.setTmnCode("TMNREC");
        partner.setSecretKey("secret");
        partner.setReturnUrl("https://merchant.test/return");
        partner.setIpnUrl("https://merchant.test/ipn");
        partner.setCreatedByEmail("owner@merchant.test");
        partner = partnerConfigRepository.save(partner);

        TestSession session = createSession(partner);
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.SUCCESS, true, "REC_OK", "00",
                recurringParams("REC_OK", "TMNREC", "00", "00"),
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.FAILED, false, "REC_FAIL", "00",
                recurringParams("REC_FAIL", "TMNREC", "24", "02"),
                "{\"RspCode\":\"00\",\"Message\":\"Confirm successful\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.ORDER_NOT_FOUND, true, "REC_MISSING", "01",
                recurringParams("REC_MISSING", "TMNREC", null, null),
                "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.ORDER_ALREADY_CONFIRMED, true, "REC_OK", "02",
                recurringParams("REC_OK", "TMNREC", null, null),
                "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.WRONG_AMOUNT, false, "REC_OK", "04",
                recurringParams("REC_OK", "TMNREC", null, null, "999"),
                "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}");
        saveSnakeCaseRun(session, partner, PaymentFlow.RECURRING, TestCaseType.INVALID_HASH, true, "REC_OK", "97",
                recurringParams("REC_OK", "TMNREC", null, null),
                "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}");

        ManualAcceptance manual = new ManualAcceptance();
        manual.setPartnerId(partner.getId());
        manual.setSessionId(session.getId());
        manual.setPartnerName(partner.getName());
        manual.setWhitelistIpPassed(true);
        manual.setLogStoragePassed(true);
        manual.setExceptionHandled(false);
        manual.setReturnSuccessTxnRef("REC_OK");
        manual.setReturnFailedTxnRef("REC_FAIL");
        manual.setReturnSuccessImage(TINY_PNG_DATA_URL);
        manual.setReturnFailedImage(TINY_PNG_DATA_URL);
        manualAcceptanceRepository.save(manual);

        byte[] content = exportMinutes(session, "2.1.1");
        String allText = readDocText(content);

        assertThat(allText).contains("TMNREC");
        assertThat(allText).contains("Merchant Recurring");
        assertThat(allText).contains("IPN URL: https://merchant.test/ipn");
        assertThat(allText).contains("Phiên bản tích hợp: 2.1.1");
        assertThat(allText).contains("REC_OK");
        assertThat(allText).contains("vnp_txn_ref (order.orderReference): REC_OK");
        assertThat(allText).contains("rspCode: \"00\"");
        assertThat(allText).contains("rspCode: \"01\"");
        assertThat(allText).contains("Message: \"Order not found\"");
        assertThat(allText).contains("Đạt");
        assertThat(allText).contains("Không đạt");
        assertThat(allText).contains("Chưa xử lý");
        assertThat(allText).contains("Màn hình thông báo:");

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            assertThat(doc.getAllPictures()).isNotEmpty();
            assertThat(DocxLayoutPolisher.countMerchantHeaderBlankLines(doc)).isZero();
        }
    }

    private TestSession createSession(PartnerConfig partner) {
        TestSession session = new TestSession();
        session.setPartnerId(partner.getId());
        session.setPartnerName(partner.getName());
        session.setTmnCode(partner.getTmnCode());
        session.setCreatedByEmail("qc@merchant.test");
        session.setStatus("OPEN");
        return testSessionRepository.save(session);
    }

    private byte[] exportMinutes(TestSession session, String integrationVersion) throws Exception {
        return mockMvc.perform(get("/api/sessions/{id}/export-minutes", session.getId())
                        .param("vnpayRepresentative", "VNPAY QA")
                        .param("integrationVersion", integrationVersion))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private void saveManualAcceptance(
            TestSession session,
            PartnerConfig partner,
            boolean whitelist,
            boolean log,
            boolean exceptionHandled
    ) {
        ManualAcceptance manual = new ManualAcceptance();
        manual.setPartnerId(partner.getId());
        manual.setSessionId(session.getId());
        manual.setPartnerName(partner.getName());
        manual.setWhitelistIpPassed(whitelist);
        manual.setLogStoragePassed(log);
        manual.setExceptionHandled(exceptionHandled);
        manualAcceptanceRepository.save(manual);
    }

    private String snakeCaseParams(String txnRef, String tmnCode, String responseCode, String transactionStatus) {
        return snakeCaseParams(txnRef, tmnCode, responseCode, transactionStatus, null);
    }

    private String snakeCaseParams(
            String txnRef,
            String tmnCode,
            String responseCode,
            String transactionStatus,
            String amount
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"vnp_txn_ref\":\"").append(txnRef).append("\"");
        sb.append(",\"vnp_tmn_code\":\"").append(tmnCode).append("\"");
        if (responseCode != null) {
            sb.append(",\"vnp_response_code\":\"").append(responseCode).append("\"");
        }
        if (transactionStatus != null) {
            sb.append(",\"vnp_transaction_status\":\"").append(transactionStatus).append("\"");
        }
        if (amount != null) {
            sb.append(",\"vnp_amount\":\"").append(amount).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String recurringParams(String txnRef, String tmnCode, String responseCode, String transactionStatus) {
        return recurringParams(txnRef, tmnCode, responseCode, transactionStatus, null);
    }

    private String recurringParams(
            String txnRef,
            String tmnCode,
            String responseCode,
            String transactionStatus,
            String amount
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"vnp_txn_ref\":\"").append(txnRef).append("\"");
        sb.append(",\"vnp_tmn_code\":\"").append(tmnCode).append("\"");
        sb.append(",\"vnp_command\":\"pay_and_create\"");
        if (responseCode != null) {
            sb.append(",\"vnp_response_code\":\"").append(responseCode).append("\"");
        }
        if (transactionStatus != null) {
            sb.append(",\"vnp_transaction_status\":\"").append(transactionStatus).append("\"");
        }
        if (amount != null) {
            sb.append(",\"vnp_amount\":\"").append(amount).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private void saveSnakeCaseRun(
            TestSession session,
            PartnerConfig partner,
            PaymentFlow flow,
            TestCaseType testCase,
            boolean passed,
            String txnRef,
            String actualRspCode,
            String requestParams,
            String responseBody
    ) {
        TestRun run = new TestRun();
        run.setSessionId(session.getId());
        run.setPartnerId(partner.getId());
        run.setPartnerName(partner.getName());
        run.setFlow(flow);
        run.setCallbackType(CallbackType.IPN);
        run.setTestCase(testCase);
        run.setTxnRef(txnRef);
        run.setTargetUrl(partner.getIpnUrl());
        run.setRequestParams(requestParams);
        run.setExpectedRspCode(actualRspCode);
        run.setActualRspCode(actualRspCode);
        run.setPassed(passed);
        run.setHttpStatus(200);
        run.setResponseBody(responseBody);
        run.setDurationMs(10L);
        run.setCreatedAt(LocalDateTime.now());
        testRunRepository.save(run);
    }

    private void saveInstalmentRun(
            TestSession session,
            PartnerConfig partner,
            TestCaseType testCase,
            boolean passed,
            String txnRef,
            String actualRspCode,
            String requestParams,
            String responseBody
    ) {
        TestRun run = new TestRun();
        run.setSessionId(session.getId());
        run.setPartnerId(partner.getId());
        run.setPartnerName(partner.getName());
        run.setFlow(PaymentFlow.INSTALMENT);
        run.setCallbackType(CallbackType.IPN);
        run.setTestCase(testCase);
        run.setTxnRef(txnRef);
        run.setTargetUrl(partner.getIpnUrl());
        run.setRequestParams(requestParams);
        run.setExpectedRspCode(actualRspCode);
        run.setActualRspCode(actualRspCode);
        run.setPassed(passed);
        run.setHttpStatus(200);
        run.setResponseBody(responseBody);
        run.setDurationMs(10L);
        run.setCreatedAt(LocalDateTime.now());
        testRunRepository.save(run);
    }

    private void saveRun(
            TestSession session,
            PartnerConfig partner,
            TestCaseType testCase,
            boolean passed,
            String txnRef
    ) {
        TestRun run = new TestRun();
        run.setSessionId(session.getId());
        run.setPartnerId(partner.getId());
        run.setPartnerName(partner.getName());
        run.setFlow(PaymentFlow.PAY);
        run.setCallbackType(CallbackType.IPN);
        run.setTestCase(testCase);
        run.setTxnRef(txnRef);
        run.setTargetUrl(partner.getIpnUrl());
        run.setRequestParams("{\"vnp_TxnRef\":\"" + txnRef + "\",\"vnp_TmnCode\":\"" + partner.getTmnCode() + "\"}");
        run.setExpectedRspCode(passed ? "00" : "01");
        run.setActualRspCode(passed ? "00" : "01");
        run.setPassed(passed);
        run.setHttpStatus(200);
        run.setResponseBody("{\"Message\":\"sample\"}");
        run.setDurationMs(10L);
        run.setCreatedAt(LocalDateTime.now());
        testRunRepository.save(run);
    }

    private String readDocText(byte[] content) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            collectBody(doc.getBodyElements(), lines);
            doc.getHeaderList().forEach(h -> h.getParagraphs().forEach(p -> lines.add(p.getText())));
            doc.getFooterList().forEach(f -> f.getParagraphs().forEach(p -> lines.add(p.getText())));
            return String.join("\n", lines);
        }
    }

    private void collectBody(List<IBodyElement> elements, List<String> lines) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                lines.add(paragraph.getText());
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collectBody(cell.getBodyElements(), lines);
                    }
                }
            }
        }
    }
}
