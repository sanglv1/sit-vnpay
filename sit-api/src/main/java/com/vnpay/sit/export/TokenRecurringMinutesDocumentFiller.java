package com.vnpay.sit.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.RecurringManualEvidenceSupport;
import com.vnpay.sit.manual.RecurringManualScenario;
import com.vnpay.sit.manual.TokenManualEvidenceSupport;
import com.vnpay.sit.manual.TokenManualScenario;
import com.vnpay.sit.manual.ManualEvidenceLogParser;
import com.vnpay.sit.manual.dto.TokenScenarioEvidence;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fills {@code VNPAYGW-Token-SIT-VN.docx} and {@code VNPAYGW-Recurring-SIT-VN.docx}.
 */
final class TokenRecurringMinutesDocumentFiller {
    private static final Logger log = LoggerFactory.getLogger(TokenRecurringMinutesDocumentFiller.class);

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MERCHANT_REQUEST_LOG_PLACEHOLDER = "[Merchant điền log request sang VNPAY tại đây]";
    private static final String MERCHANT_RESPONSE_LOG_PLACEHOLDER = "[Merchant điền log kết quả phản hồi từ VNPAY tại đây]";
    private static final String MERCHANT_SCREENSHOT_PLACEHOLDER = "[Merchant chụp và dán ảnh tại đây]";
    private static final String RECURRING_RESPONSE_MARKER = "Dữ liệu VNPAY trả về:";
    private static final String TOKEN_RESPONSE_MARKER = "Dữ liệu VNPAY phản hồi:";

    private final ObjectMapper objectMapper;
    private final MinutesViewModelMapper viewModelMapper;

    TokenRecurringMinutesDocumentFiller(ObjectMapper objectMapper, MinutesViewModelMapper viewModelMapper) {
        this.objectMapper = objectMapper;
        this.viewModelMapper = viewModelMapper;
    }

    void fill(XWPFDocument document, MinutesExportContext ctx) {
        applyTemplateTokens(document, viewModelMapper.map(ctx));
        applyHeader(document, ctx);
        fillBody(document, ctx);
        fillEvaluationColumn(document, ctx);
        DocxLayoutPolisher.polish(document);
        fillSignatures(document, ctx);
    }

    private void applyHeader(XWPFDocument document, MinutesExportContext ctx) {
        LocalDateTime when = ctx.getSession().getCreatedAt() != null
                ? ctx.getSession().getCreatedAt()
                : LocalDateTime.now();
        String dateText = "Hà Nội ,ngày: " + when.format(HEADER_DATE);

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }
            String updated = text;
            if (text.contains("ngày:") && text.contains("tháng:") && text.contains("năm:")) {
                updated = dateText;
            } else if (text.startsWith("Tên merchant:")) {
                updated = "Tên merchant: " + ctx.getPartner().getName();
            } else if (text.startsWith("Mã định danh kết nối:")) {
                updated = "Mã định danh kết nối: " + ctx.getPartner().getTmnCode();
            } else if (text.startsWith("Tên website/app kết nối:")) {
                updated = "Tên website/app kết nối:";
            } else if (text.startsWith("Phiên bản tích hợp:")) {
                updated = "Phiên bản tích hợp: " + integrationVersion(ctx);
            } else if (text.startsWith("Link test:")) {
                updated = "IPN URL: " + ipnTestUrl(ctx);
            } else if (text.startsWith("Đại diện VNPAY:")) {
                updated = "Đại diện VNPAY: " + blank(ctx.getVnpayRepresentative());
            } else if (text.startsWith("Đại diện merchant:")) {
                updated = "Đại diện merchant: " + ctx.resolvedMerchantRepresentative();
            }
            if (!updated.equals(text)) {
                DocxParagraphWalker.setParagraphText(paragraph, updated);
            }
        }
    }

    private void fillBody(XWPFDocument document, MinutesExportContext ctx) {
        boolean inIpn = false;
        TokenManualScenario currentTokenScenario = null;
        RecurringManualScenario currentRecurringScenario = null;
        TestCaseType currentCase = null;
        boolean inInput = false;
        boolean inOutput = false;
        Map<RecurringManualScenario, Map<String, String>> recurringRequestParams = new EnumMap<>(RecurringManualScenario.class);
        Map<RecurringManualScenario, Map<String, String>> recurringResponseParams = new EnumMap<>(RecurringManualScenario.class);
        Map<TokenManualScenario, Map<String, String>> tokenResponseParams = new EnumMap<>(TokenManualScenario.class);

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }

            if (ctx.flow() == PaymentFlow.TOKEN) {
                Optional<TokenManualScenario> tokenScenario = TokenManualScenario.bySituationPrefix(text);
                if (tokenScenario.isPresent()) {
                    currentTokenScenario = tokenScenario.get();
                    currentRecurringScenario = null;
                    inIpn = false;
                    inInput = false;
                    inOutput = false;
                    continue;
                }
            }
            if (ctx.flow() == PaymentFlow.RECURRING) {
                Optional<RecurringManualScenario> recurringScenario = RecurringManualScenario.bySituationPrefix(text);
                if (recurringScenario.isPresent()) {
                    currentRecurringScenario = recurringScenario.get();
                    currentTokenScenario = null;
                    inIpn = false;
                    inInput = false;
                    inOutput = false;
                    continue;
                }
            }
            if (text.contains("IPN URL")) {
                inIpn = true;
                currentTokenScenario = null;
                currentRecurringScenario = null;
                currentCase = null;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.contains("Quy định khác")) {
                inIpn = false;
                currentTokenScenario = null;
                currentRecurringScenario = null;
                currentCase = null;
                inInput = false;
                inOutput = false;
            }

            if (inIpn) {
                currentCase = detectIpnCase(text, ctx.flow()).orElse(currentCase);
            }

            if (text.equalsIgnoreCase("Input:")) {
                inInput = true;
                inOutput = false;
                continue;
            }
            if (text.equalsIgnoreCase("Output:")) {
                inOutput = true;
                inInput = false;
                continue;
            }

            if (currentTokenScenario != null && inInput && text.contains(MERCHANT_REQUEST_LOG_PLACEHOLDER)) {
                if (fillTokenScenarioRequestLog(paragraph, ctx, currentTokenScenario)) {
                    continue;
                }
            }

            if (currentTokenScenario != null && text.startsWith(TOKEN_RESPONSE_MARKER)) {
                if (fillTokenScenarioResponseSummary(paragraph, ctx, currentTokenScenario)) {
                    continue;
                }
            }

            if (currentTokenScenario != null && inOutput && text.contains(MERCHANT_RESPONSE_LOG_PLACEHOLDER)) {
                if (fillTokenScenarioResponseLog(paragraph, ctx, currentTokenScenario)) {
                    continue;
                }
            }

            if (currentTokenScenario != null && inOutput && ManualEvidenceLogParser.isTemplateFieldLine(text)) {
                Map<String, String> params = tokenResponseParams.computeIfAbsent(
                        currentTokenScenario,
                        scenario -> ManualEvidenceLogParser.parse(
                                tokenScenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog))
                );
                if (fillEvidenceFieldLine(paragraph, text, params)) {
                    continue;
                }
            }

            if (currentTokenScenario != null && text.contains(MERCHANT_SCREENSHOT_PLACEHOLDER)) {
                if (fillTokenScenarioScreenshot(paragraph, ctx, currentTokenScenario)) {
                    continue;
                }
            }

            if (currentRecurringScenario != null && inInput && ManualEvidenceLogParser.isTemplateFieldLine(text)) {
                Map<String, String> params = recurringRequestParams.computeIfAbsent(
                        currentRecurringScenario,
                        scenario -> ManualEvidenceLogParser.parse(
                                recurringScenarioField(ctx, scenario, TokenScenarioEvidence::getRequestLog))
                );
                if (fillEvidenceFieldLine(paragraph, text, params)) {
                    continue;
                }
            }

            if (currentRecurringScenario != null && inOutput && ManualEvidenceLogParser.isTemplateFieldLine(text)) {
                Map<String, String> params = recurringResponseParams.computeIfAbsent(
                        currentRecurringScenario,
                        scenario -> ManualEvidenceLogParser.parse(
                                recurringScenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog))
                );
                if (fillEvidenceFieldLine(paragraph, text, params)) {
                    continue;
                }
            }

            if (currentRecurringScenario != null && text.startsWith(RECURRING_RESPONSE_MARKER)) {
                if (fillRecurringScenarioResponseLog(paragraph, ctx, currentRecurringScenario)) {
                    continue;
                }
            }

            if (currentRecurringScenario != null && text.startsWith("Màn hình thông báo:")) {
                if (fillRecurringScenarioScreenshot(paragraph, ctx, currentRecurringScenario, text)) {
                    continue;
                }
            }

            if (inIpn && inInput && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (run.isPresent() && fillSnakeCaseParamLine(paragraph, text, parseParams(run.get()))) {
                    continue;
                }
            }

            if (inIpn && inOutput && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (text.startsWith("rspCode:")) {
                    String rsp = currentCase == TestCaseType.UNKNOWN_ERROR
                            ? "99"
                            : run.map(TestRun::getActualRspCode).map(this::blank).orElse("");
                    DocxParagraphWalker.setParagraphText(paragraph, "rspCode: " + quote(rsp));
                    continue;
                }
                if (text.startsWith("Message:")) {
                    String message = run.map(TestRun::getResponseBody)
                            .map(this::extractMessage)
                            .filter(s -> !s.isBlank())
                            .orElse(defaultStatusMessage(currentCase));
                    DocxParagraphWalker.setParagraphText(paragraph, "Message: " + quote(message));
                }
            }
        }
    }

    private void fillEvaluationColumn(XWPFDocument document, MinutesExportContext ctx) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        FlowProfile profile = profile(ctx.flow());
        DocxParagraphWalker.forEachTableRow(document, row -> {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 5) {
                return;
            }
            String caseNo = normalize(cells.get(0).getText());
            String situation = normalize(cells.get(1).getText());
            String evaluation = resolveEvaluation(caseNo, situation, ctx, manual, profile);
            if (!evaluation.isBlank()) {
                DocxParagraphWalker.setCellText(cells.get(4), evaluation);
            }
        });
    }

    private String resolveEvaluation(
            String caseNo,
            String situation,
            MinutesExportContext ctx,
            ManualAcceptance manual,
            FlowProfile profile
    ) {
        if (caseNo.equals(profile.exceptionCaseNo()) && manual != null) {
            return exceptionEvaluation(manual.getExceptionHandled());
        }
        if (caseNo.equals(profile.whitelistCaseNo()) && manual != null) {
            return passEvaluation(manual.getWhitelistIpPassed());
        }
        if (caseNo.equals(profile.logCaseNo()) && manual != null) {
            return passEvaluation(manual.getLogStoragePassed());
        }
        if (ctx.flow() == PaymentFlow.TOKEN && manual != null) {
            Optional<TokenManualScenario> scenario = TokenManualScenario.byCaseNo(caseNo);
            if (scenario.isPresent()) {
                return passEvaluation(TokenManualEvidenceSupport.passesEvaluation(manual, scenario.get(), objectMapper));
            }
        }
        if (ctx.flow() == PaymentFlow.RECURRING && manual != null) {
            Optional<RecurringManualScenario> scenario = RecurringManualScenario.byCaseNo(caseNo);
            if (scenario.isPresent()) {
                return passEvaluation(RecurringManualEvidenceSupport.passesEvaluation(manual, scenario.get(), objectMapper));
            }
        }
        return profile.mapAutoCase(caseNo, situation)
                .flatMap(ctx::run)
                .map(run -> passEvaluation(run.isPassed()))
                .orElse("");
    }

    private void fillSignatures(XWPFDocument document, MinutesExportContext ctx) {
        boolean afterVnpayHeading = false;
        boolean afterMerchantHeading = false;

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.equals("ĐẠI DIỆN VNPAY")) {
                afterVnpayHeading = true;
                afterMerchantHeading = false;
                continue;
            }
            if (text.equals("ĐẠI DIỆN Merchant")) {
                DocxParagraphWalker.setParagraphText(paragraph, "ĐẠI DIỆN " + ctx.getPartner().getName());
                afterMerchantHeading = true;
                afterVnpayHeading = false;
                continue;
            }
            if (!text.isEmpty() && afterVnpayHeading) {
                DocxParagraphWalker.setParagraphText(paragraph, blank(ctx.getVnpayRepresentative()));
                afterVnpayHeading = false;
                continue;
            }
            if (!text.isEmpty() && afterMerchantHeading) {
                DocxParagraphWalker.setParagraphText(paragraph, ctx.resolvedMerchantRepresentative());
                afterMerchantHeading = false;
            }
        }
    }

    private boolean fillRecurringScenarioResponseLog(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            RecurringManualScenario scenario
    ) {
        String responseLog = recurringScenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog);
        if (responseLog.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, RECURRING_RESPONSE_MARKER + " " + responseLog.trim());
        return true;
    }

    private boolean fillRecurringScenarioScreenshot(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            RecurringManualScenario scenario,
            String captionPrefix
    ) {
        String image = recurringScenarioField(ctx, scenario, TokenScenarioEvidence::getImage);
        if (DocxImageInserter.embedDataUrlImage(paragraph, image, captionPrefix)) {
            return true;
        }
        String txnRef = extractTxnRefFromRequest(ctx, scenario);
        if (!txnRef.isBlank()) {
            DocxParagraphWalker.setParagraphText(paragraph, captionPrefix + " (TxnRef: " + txnRef + ")");
            return true;
        }
        return false;
    }

    private String extractTxnRefFromRequest(MinutesExportContext ctx, RecurringManualScenario scenario) {
        String requestLog = recurringScenarioField(ctx, scenario, TokenScenarioEvidence::getRequestLog);
        if (requestLog.isBlank()) {
            ManualAcceptance manual = ctx.getManualAcceptance();
            if (manual == null) {
                return "";
            }
            if (scenario == RecurringManualScenario.CARD_VERIFY_SUCCESS) {
                return blank(manual.getReturnSuccessTxnRef());
            }
            if (scenario == RecurringManualScenario.CARD_VERIFY_FAILED) {
                return blank(manual.getReturnFailedTxnRef());
            }
            return "";
        }
        return blank(ManualEvidenceLogParser.extractTxnRef(requestLog));
    }

    private String recurringScenarioField(
            MinutesExportContext ctx,
            RecurringManualScenario scenario,
            java.util.function.Function<TokenScenarioEvidence, String> extractor
    ) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return "";
        }
        return RecurringManualEvidenceSupport.evidence(manual, scenario, objectMapper)
                .map(extractor)
                .map(this::blank)
                .orElse("");
    }

    private boolean fillEvidenceFieldLine(XWPFParagraph paragraph, String text, Map<String, String> params) {
        Optional<String> label = ManualEvidenceLogParser.templateFieldLabel(text);
        if (label.isEmpty()) {
            return false;
        }
        String value = ManualEvidenceLogParser.lookup(params, label.get());
        if (value == null || value.isBlank()) {
            return false;
        }
        String formatted = ManualEvidenceLogParser.formatFieldLine(label.get(), value);
        if (formatted == null) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, formatted);
        return true;
    }

    private boolean fillTokenScenarioRequestLog(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            TokenManualScenario scenario
    ) {
        String requestLog = tokenScenarioField(ctx, scenario, TokenScenarioEvidence::getRequestLog);
        if (requestLog.isBlank()) {
            return false;
        }
        Map<String, String> params = ManualEvidenceLogParser.parse(requestLog);
        String formatted = ManualEvidenceLogParser.formatAsFieldLines(params);
        DocxParagraphWalker.setParagraphText(paragraph, formatted.isBlank() ? requestLog.trim() : formatted);
        return true;
    }

    private boolean fillTokenScenarioResponseSummary(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            TokenManualScenario scenario
    ) {
        String responseLog = tokenScenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog);
        if (responseLog.isBlank()) {
            return false;
        }
        String formatted = ManualEvidenceLogParser.formatAsFieldLines(ManualEvidenceLogParser.parse(responseLog));
        if (formatted.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, TOKEN_RESPONSE_MARKER + "\n" + formatted);
        return true;
    }

    private boolean fillTokenScenarioResponseLog(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            TokenManualScenario scenario
    ) {
        String responseLog = tokenScenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog);
        if (responseLog.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, responseLog.trim());
        return true;
    }

    private boolean fillTokenScenarioScreenshot(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            TokenManualScenario scenario
    ) {
        String image = tokenScenarioField(ctx, scenario, TokenScenarioEvidence::getImage);
        return DocxImageInserter.embedDataUrlImage(paragraph, image, null);
    }

    private String tokenScenarioField(
            MinutesExportContext ctx,
            TokenManualScenario scenario,
            java.util.function.Function<TokenScenarioEvidence, String> extractor
    ) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return "";
        }
        return TokenManualEvidenceSupport.evidence(manual, scenario, objectMapper)
                .map(extractor)
                .map(this::blank)
                .orElse("");
    }

    private boolean fillSnakeCaseParamLine(XWPFParagraph paragraph, String text, Map<String, String> params) {
        int colon = text.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String label = text.substring(0, colon).trim();
        String paramKey = mapLabelToParamKey(label);
        if (paramKey == null) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, label + ": " + params.getOrDefault(paramKey, ""));
        return true;
    }

    private String mapLabelToParamKey(String label) {
        if (label.startsWith("vnp_txn_ref")) {
            return "vnp_txn_ref";
        }
        if (label.startsWith("vnp_")) {
            return label;
        }
        return null;
    }

    private Optional<TestCaseType> detectIpnCase(String text, PaymentFlow flow) {
        if (flow == PaymentFlow.RECURRING) {
            if (text.startsWith("Giao dịch xác thực thẻ thành công")) {
                return Optional.of(TestCaseType.SUCCESS);
            }
            if (text.startsWith("Giao dịch xác thực thẻ không thành công")) {
                return Optional.of(TestCaseType.FAILED);
            }
        } else {
            if (text.startsWith("Giao dịch thành công")) {
                return Optional.of(TestCaseType.SUCCESS);
            }
            if (text.startsWith("Giao dịch không thành công") || text.startsWith("Giao dịch thất bại")) {
                return Optional.of(TestCaseType.FAILED);
            }
        }
        if (text.startsWith("Giao dịch đã được confirm")) {
            return Optional.of(TestCaseType.ORDER_ALREADY_CONFIRMED);
        }
        if (text.startsWith("Không tìm thấy giao dịch confirm")) {
            return Optional.of(TestCaseType.ORDER_NOT_FOUND);
        }
        if (text.startsWith("Số tiền không hợp lệ")) {
            return Optional.of(TestCaseType.WRONG_AMOUNT);
        }
        if (text.startsWith("Chữ ký không hợp lệ")) {
            return Optional.of(TestCaseType.INVALID_HASH);
        }
        if (text.startsWith("Lỗi ngoại lệ khác")) {
            return Optional.of(TestCaseType.UNKNOWN_ERROR);
        }
        return Optional.empty();
    }

    private FlowProfile profile(PaymentFlow flow) {
        return flow == PaymentFlow.RECURRING ? FlowProfile.RECURRING : FlowProfile.TOKEN;
    }

    private enum FlowProfile {
        TOKEN(
                Map.of(
                        "9", TestCaseType.SUCCESS,
                        "10", TestCaseType.FAILED,
                        "11", TestCaseType.ORDER_NOT_FOUND,
                        "12", TestCaseType.ORDER_ALREADY_CONFIRMED,
                        "13", TestCaseType.WRONG_AMOUNT,
                        "14", TestCaseType.INVALID_HASH
                ),
                "15", "16", "17"
        ),
        RECURRING(
                Map.of(
                        "15", TestCaseType.SUCCESS,
                        "16", TestCaseType.FAILED,
                        "17", TestCaseType.ORDER_NOT_FOUND,
                        "18", TestCaseType.ORDER_ALREADY_CONFIRMED,
                        "19", TestCaseType.WRONG_AMOUNT,
                        "20", TestCaseType.INVALID_HASH
                ),
                "21", "22", "23"
        );

        private final Map<String, TestCaseType> autoCases;
        private final String exceptionCaseNo;
        private final String whitelistCaseNo;
        private final String logCaseNo;

        FlowProfile(Map<String, TestCaseType> autoCases, String exceptionCaseNo, String whitelistCaseNo, String logCaseNo) {
            this.autoCases = autoCases;
            this.exceptionCaseNo = exceptionCaseNo;
            this.whitelistCaseNo = whitelistCaseNo;
            this.logCaseNo = logCaseNo;
        }

        String exceptionCaseNo() {
            return exceptionCaseNo;
        }

        String whitelistCaseNo() {
            return whitelistCaseNo;
        }

        String logCaseNo() {
            return logCaseNo;
        }

        Optional<TestCaseType> mapAutoCase(String caseNo, String situation) {
            TestCaseType mapped = autoCases.get(caseNo);
            if (mapped != null) {
                return Optional.of(mapped);
            }
            return mapAutoCaseBySituation(situation);
        }

        private Optional<TestCaseType> mapAutoCaseBySituation(String situation) {
            if (this == RECURRING) {
                if (situation.startsWith("Giao dịch xác thực thẻ thành công")) {
                    return Optional.of(TestCaseType.SUCCESS);
                }
                if (situation.startsWith("Giao dịch xác thực thẻ không thành công")) {
                    return Optional.of(TestCaseType.FAILED);
                }
            } else {
                if (situation.startsWith("Giao dịch thành công")) {
                    return Optional.of(TestCaseType.SUCCESS);
                }
                if (situation.startsWith("Giao dịch không thành công")) {
                    return Optional.of(TestCaseType.FAILED);
                }
            }
            return Optional.empty();
        }
    }

    private String exceptionEvaluation(Boolean handled) {
        if (handled == null) {
            return "";
        }
        return Boolean.TRUE.equals(handled) ? "Đã xử lý" : "Chưa xử lý";
    }

    private String passEvaluation(Boolean passed) {
        if (passed == null) {
            return "";
        }
        return Boolean.TRUE.equals(passed) ? "Đạt" : "Không đạt";
    }

    private Map<String, String> parseParams(TestRun run) {
        try {
            return objectMapper.readValue(run.getRequestParams(), new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Cannot parse requestParams for testRunId={}, payload={}", run.getId(), run.getRequestParams(), ex);
            return Map.of();
        }
    }

    private void applyTemplateTokens(XWPFDocument document, MinutesViewModelMapper.MinutesViewModel viewModel) {
        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String updated = text;
            for (Map.Entry<String, String> token : viewModel.tokens().entrySet()) {
                updated = updated.replace("${" + token.getKey() + "}", token.getValue());
            }
            if (!updated.equals(text)) {
                DocxParagraphWalker.setParagraphText(paragraph, updated);
            }
        }
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("Message")) {
                return node.get("Message").asText("");
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "";
    }

    private String defaultStatusMessage(TestCaseType testCase) {
        return switch (testCase) {
            case SUCCESS -> "Confirm successful";
            case ORDER_ALREADY_CONFIRMED -> "Order already confirmed";
            case FAILED -> "Confirm successful";
            case ORDER_NOT_FOUND -> "Order not found";
            case WRONG_AMOUNT -> "Invalid amount";
            case INVALID_HASH -> "Invalid signature";
            case UNKNOWN_ERROR -> "Unknow error";
        };
    }

    private String quote(String value) {
        if (value == null || value.isBlank()) {
            return "\"\"";
        }
        return "\"" + value.trim() + "\"";
    }

    private String ipnTestUrl(MinutesExportContext ctx) {
        return blank(ctx.getPartner().getIpnUrl());
    }

    private String integrationVersion(MinutesExportContext ctx) {
        if (ctx.getIntegrationVersion() != null && !ctx.getIntegrationVersion().isBlank()) {
            return ctx.getIntegrationVersion().trim();
        }
        return ctx.flow() == PaymentFlow.TOKEN ? "2.1.0" : "2.1.1";
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}
