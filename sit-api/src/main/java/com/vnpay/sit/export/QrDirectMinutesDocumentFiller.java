package com.vnpay.sit.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.ManualEvidenceLogParser;
import com.vnpay.sit.manual.QrDirectManualEvidenceSupport;
import com.vnpay.sit.manual.QrDirectManualScenario;
import com.vnpay.sit.manual.dto.TokenScenarioEvidence;
import com.vnpay.sit.manual.entity.ManualAcceptance;
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

/** Fills {@code VNPAYGW-QRDirect-SIT-VN.docx}. */
final class QrDirectMinutesDocumentFiller {
    private static final Logger log = LoggerFactory.getLogger(QrDirectMinutesDocumentFiller.class);

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String RESPONSE_MARKER = "Dữ liệu VNPAY phản hồi:";
    private static final String MERCHANT_REQUEST_LOG_PLACEHOLDER = "[Merchant điền log request sang VNPAY tại đây]";
    private static final String MERCHANT_RESPONSE_LOG_PLACEHOLDER =
            "[Merchant điền log kết quả phản hồi từ API của VNPAY tại đây]";
    private static final String MERCHANT_SCREENSHOT_PLACEHOLDER = "[Merchant chụp và dán ảnh tại đây]";

    private final ObjectMapper objectMapper;
    private final MinutesViewModelMapper viewModelMapper;

    QrDirectMinutesDocumentFiller(ObjectMapper objectMapper, MinutesViewModelMapper viewModelMapper) {
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
                updated = "Tên website/app kết nối: " + blank(ctx.getWebsiteName());
            } else if (text.startsWith("Dịch vụ kết nối:")) {
                updated = "Dịch vụ kết nối: merchant hosted QRcode";
            } else if (text.startsWith("Môi trường kiểm tra:")) {
                updated = "Môi trường kiểm tra: sandbox";
            } else if (text.startsWith("Phiên bản tích hợp:")) {
                updated = "Phiên bản tích hợp: " + integrationVersion(ctx);
            } else if (text.startsWith("Link test:")) {
                updated = "Link test: " + ipnTestUrl(ctx);
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
        boolean inDeepLinkSection = false;
        boolean inIpn = false;
        QrDirectManualScenario currentScenario = null;
        TestCaseType currentCase = null;
        boolean inInput = false;
        boolean inOutput = false;
        Map<QrDirectManualScenario, Map<String, String>> requestParams = new EnumMap<>(QrDirectManualScenario.class);
        Map<QrDirectManualScenario, Map<String, String>> responseParams = new EnumMap<>(QrDirectManualScenario.class);

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }

            if (text.contains("Deep-link")) {
                inDeepLinkSection = true;
                continue;
            }
            Optional<QrDirectManualScenario> scenario = detectScenario(text, inDeepLinkSection);
            if (scenario.isPresent()) {
                currentScenario = scenario.get();
                inIpn = false;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.contains("IPN URL")) {
                inIpn = true;
                inDeepLinkSection = false;
                currentScenario = null;
                currentCase = null;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.contains("Quy định khác")) {
                inIpn = false;
                currentScenario = null;
                currentCase = null;
                inInput = false;
                inOutput = false;
            }

            if (inIpn) {
                currentCase = detectIpnCase(text).orElse(currentCase);
            }

            if (text.equalsIgnoreCase("Input:")) {
                inInput = true;
                inOutput = false;
                continue;
            }
            if (text.startsWith("Output:")) {
                inOutput = true;
                inInput = false;
                continue;
            }

            if (currentScenario != null && inInput && text.contains(MERCHANT_REQUEST_LOG_PLACEHOLDER)) {
                if (fillScenarioRequestLog(paragraph, ctx, currentScenario)) {
                    continue;
                }
            }
            if (currentScenario != null && text.startsWith(RESPONSE_MARKER)) {
                if (fillScenarioResponseLog(paragraph, ctx, currentScenario)) {
                    continue;
                }
            }
            if (currentScenario != null && inOutput && text.contains(MERCHANT_RESPONSE_LOG_PLACEHOLDER)) {
                if (fillScenarioResponseLogBlock(paragraph, ctx, currentScenario)) {
                    continue;
                }
            }
            if (currentScenario != null && inInput && ManualEvidenceLogParser.isTemplateFieldLine(text)) {
                QrDirectManualScenario active = currentScenario;
                Map<String, String> params = requestParams.computeIfAbsent(
                        active,
                        ignored -> ManualEvidenceLogParser.parse(
                                scenarioField(ctx, active, TokenScenarioEvidence::getRequestLog))
                );
                if (fillEvidenceFieldLine(paragraph, text, params)) {
                    continue;
                }
            }
            if (currentScenario != null && inOutput && ManualEvidenceLogParser.isTemplateFieldLine(text)) {
                QrDirectManualScenario active = currentScenario;
                Map<String, String> params = responseParams.computeIfAbsent(
                        active,
                        ignored -> ManualEvidenceLogParser.parse(
                                scenarioField(ctx, active, TokenScenarioEvidence::getResponseLog))
                );
                if (fillEvidenceFieldLine(paragraph, text, params)) {
                    continue;
                }
            }
            if (currentScenario != null && text.contains(MERCHANT_SCREENSHOT_PLACEHOLDER)) {
                if (fillScenarioScreenshot(paragraph, ctx, currentScenario, "Màn hình hiển thị mã QRcode thanh toán:")) {
                    continue;
                }
            }
            if (currentScenario != null && text.startsWith("Màn hình")) {
                if (fillScenarioScreenshot(paragraph, ctx, currentScenario, text)) {
                    continue;
                }
            }

            if (inIpn && inInput && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (run.isPresent() && fillPascalCaseParamLine(paragraph, text, parseParams(run.get()))) {
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

    private Optional<QrDirectManualScenario> detectScenario(String text, boolean inDeepLinkSection) {
        if (text.startsWith("Tạo yêu cầu thanh toán thành công")) {
            return Optional.of(inDeepLinkSection
                    ? QrDirectManualScenario.DEEPLINK_CREATE_SUCCESS
                    : QrDirectManualScenario.CREATE_PAY_SUCCESS);
        }
        if (text.startsWith("Tạo yêu cầu thanh toán không thành công")) {
            return Optional.of(inDeepLinkSection
                    ? QrDirectManualScenario.DEEPLINK_CREATE_FAILED
                    : QrDirectManualScenario.CREATE_PAY_FAILED);
        }
        return QrDirectManualScenario.bySituationPrefix(text);
    }

    private void fillEvaluationColumn(XWPFDocument document, MinutesExportContext ctx) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        DocxParagraphWalker.forEachTableRow(document, row -> {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 5) {
                return;
            }
            String caseNo = normalize(cells.get(0).getText());
            String situation = normalize(cells.get(1).getText());
            String evaluation = resolveEvaluation(caseNo, situation, ctx, manual);
            if (!evaluation.isBlank()) {
                DocxParagraphWalker.setCellText(cells.get(4), evaluation);
            }
        });
    }

    private String resolveEvaluation(
            String caseNo,
            String situation,
            MinutesExportContext ctx,
            ManualAcceptance manual
    ) {
        if ("18".equals(caseNo) && manual != null) {
            return passEvaluation(manual.getWhitelistIpPassed());
        }
        if ("19".equals(caseNo) && manual != null) {
            return passEvaluation(manual.getLogStoragePassed());
        }
        if (manual != null) {
            Optional<QrDirectManualScenario> scenario = QrDirectManualScenario.byCaseNo(caseNo);
            if (scenario.isPresent()) {
                return passEvaluation(QrDirectManualEvidenceSupport.passesEvaluation(
                        manual, scenario.get(), objectMapper));
            }
        }
        return mapAutoCase(caseNo, situation)
                .flatMap(ctx::run)
                .map(run -> passEvaluation(run.isPassed()))
                .orElse("");
    }

    private Optional<TestCaseType> mapAutoCase(String caseNo, String situation) {
        return switch (caseNo) {
            case "13" -> Optional.of(TestCaseType.ORDER_NOT_FOUND);
            case "14" -> Optional.of(TestCaseType.ORDER_ALREADY_CONFIRMED);
            case "15" -> Optional.of(TestCaseType.WRONG_AMOUNT);
            case "16" -> Optional.of(TestCaseType.INVALID_HASH);
            case "17" -> Optional.of(TestCaseType.UNKNOWN_ERROR);
            default -> mapAutoCaseBySituation(situation);
        };
    }

    private Optional<TestCaseType> mapAutoCaseBySituation(String situation) {
        if (situation.startsWith("Giao dịch thành công")) {
            return Optional.of(TestCaseType.SUCCESS);
        }
        if (situation.startsWith("Giao dịch không thành công")) {
            return Optional.of(TestCaseType.FAILED);
        }
        return Optional.empty();
    }

    private boolean fillScenarioRequestLog(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            QrDirectManualScenario scenario
    ) {
        String requestLog = scenarioField(ctx, scenario, TokenScenarioEvidence::getRequestLog);
        if (requestLog.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, requestLog.trim());
        return true;
    }

    private boolean fillScenarioResponseLog(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            QrDirectManualScenario scenario
    ) {
        String responseLog = scenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog);
        if (responseLog.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, RESPONSE_MARKER + " " + responseLog.trim());
        return true;
    }

    private boolean fillScenarioResponseLogBlock(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            QrDirectManualScenario scenario
    ) {
        String responseLog = scenarioField(ctx, scenario, TokenScenarioEvidence::getResponseLog);
        if (responseLog.isBlank()) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, responseLog.trim());
        return true;
    }

    private boolean fillScenarioScreenshot(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            QrDirectManualScenario scenario,
            String captionPrefix
    ) {
        String image = scenarioField(ctx, scenario, TokenScenarioEvidence::getImage);
        if (DocxImageInserter.embedDataUrlImage(paragraph, image, captionPrefix)) {
            return true;
        }
        String txnRef = blank(ManualEvidenceLogParser.extractTxnRef(
                scenarioField(ctx, scenario, TokenScenarioEvidence::getRequestLog)));
        if (!txnRef.isBlank()) {
            DocxParagraphWalker.setParagraphText(paragraph, captionPrefix + " (TxnRef: " + txnRef + ")");
            return true;
        }
        return false;
    }

    private String scenarioField(
            MinutesExportContext ctx,
            QrDirectManualScenario scenario,
            java.util.function.Function<TokenScenarioEvidence, String> extractor
    ) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return "";
        }
        return QrDirectManualEvidenceSupport.evidence(manual, scenario, objectMapper)
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

    private boolean fillPascalCaseParamLine(XWPFParagraph paragraph, String text, Map<String, String> params) {
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
        if (label.startsWith("vnp_TxnRef")) {
            return "vnp_TxnRef";
        }
        if (label.startsWith("vnp_")) {
            return label;
        }
        return null;
    }

    private Optional<TestCaseType> detectIpnCase(String text) {
        if (text.startsWith("Giao dịch thành công")) {
            return Optional.of(TestCaseType.SUCCESS);
        }
        if (text.startsWith("Giao dịch không thành công")) {
            return Optional.of(TestCaseType.FAILED);
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

    private Map<String, String> parseParams(TestRun run) {
        try {
            return objectMapper.readValue(run.getRequestParams(), new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Cannot parse requestParams for testRunId={}", run.getId(), ex);
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

    private String passEvaluation(Boolean passed) {
        if (passed == null) {
            return "";
        }
        return Boolean.TRUE.equals(passed) ? "Đạt" : "Không đạt";
    }

    private String quote(String value) {
        if (value == null || value.isBlank()) {
            return "\"\"";
        }
        return "\"" + value.trim() + "\"";
    }

    private String ipnTestUrl(MinutesExportContext ctx) {
        if (ctx.getTestLink() != null && !ctx.getTestLink().isBlank()) {
            return ctx.getTestLink().trim();
        }
        return blank(ctx.getPartner().getIpnUrl());
    }

    private String integrationVersion(MinutesExportContext ctx) {
        if (ctx.getIntegrationVersion() != null && !ctx.getIntegrationVersion().isBlank()) {
            return ctx.getIntegrationVersion().trim();
        }
        return "2.1.0";
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}
