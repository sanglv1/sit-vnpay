package com.vnpay.sit.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fills {@code VNPAYGW-Installment-SIT-VN.docx} — table layout with instalment API + IPN cases.
 */
final class InstalmentMinutesDocumentFiller {
    private static final Logger log = LoggerFactory.getLogger(InstalmentMinutesDocumentFiller.class);

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;
    private final MinutesViewModelMapper viewModelMapper;

    InstalmentMinutesDocumentFiller(ObjectMapper objectMapper, MinutesViewModelMapper viewModelMapper) {
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
        boolean inReturnPayment = false;
        boolean returnSuccess = false;
        boolean returnFailed = false;
        TestCaseType currentCase = null;
        boolean inInput = false;
        boolean inOutput = false;

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }

            if (text.startsWith("Thanh toán trả góp thành công")) {
                inReturnPayment = true;
                inIpn = false;
                returnSuccess = true;
                returnFailed = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.startsWith("Thanh toán trả góp không thành công")) {
                inReturnPayment = true;
                inIpn = false;
                returnFailed = true;
                returnSuccess = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.contains("IPN URL")) {
                inIpn = true;
                inReturnPayment = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
                continue;
            }
            if (text.contains("Quy định khác")) {
                inIpn = false;
                inReturnPayment = false;
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
            if (text.equalsIgnoreCase("Output:")) {
                inOutput = true;
                inInput = false;
                continue;
            }

            if (inReturnPayment && inOutput) {
                Optional<TestRun> run = returnSuccess
                        ? ctx.run(TestCaseType.SUCCESS)
                        : ctx.run(TestCaseType.FAILED);
                if (run.isPresent() && fillInstalmentParamLine(paragraph, text, parseParams(run.get()))) {
                    continue;
                }
            }

            if (inReturnPayment && text.startsWith("Màn hình thông báo:")) {
                if (embedReturnScreenImage(paragraph, ctx, returnSuccess, returnFailed)) {
                    continue;
                }
            }

            if (inReturnPayment && inInput && text.startsWith("tmnCode:")) {
                DocxParagraphWalker.setParagraphText(paragraph, "tmnCode: " + ctx.getPartner().getTmnCode());
                continue;
            }

            if (inIpn && inInput && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (run.isPresent() && fillInstalmentParamLine(paragraph, text, parseParams(run.get()))) {
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
                    continue;
                }
            }
        }
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
        if ("25".equals(caseNo) && manual != null) {
            return exceptionEvaluation(manual.getExceptionHandled());
        }
        if ("26".equals(caseNo) && manual != null) {
            return passEvaluation(manual.getWhitelistIpPassed());
        }
        if ("27".equals(caseNo) && manual != null) {
            return passEvaluation(manual.getLogStoragePassed());
        }
        return mapAutoCase(caseNo, situation)
                .flatMap(ctx::run)
                .map(run -> passEvaluation(run.isPassed()))
                .orElse("");
    }

    private Optional<TestCaseType> mapAutoCase(String caseNo, String situation) {
        return switch (caseNo) {
            case "21" -> Optional.of(TestCaseType.ORDER_NOT_FOUND);
            case "22" -> Optional.of(TestCaseType.ORDER_ALREADY_CONFIRMED);
            case "23" -> Optional.of(TestCaseType.WRONG_AMOUNT);
            case "24" -> Optional.of(TestCaseType.INVALID_HASH);
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

    private boolean embedReturnScreenImage(
            XWPFParagraph paragraph,
            MinutesExportContext ctx,
            boolean returnSuccess,
            boolean returnFailed
    ) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return false;
        }
        String imageDataUrl = returnSuccess
                ? manual.getReturnSuccessImage()
                : returnFailed ? manual.getReturnFailedImage() : null;
        if (DocxImageInserter.embedDataUrlImage(paragraph, imageDataUrl, "Màn hình thông báo:")) {
            return true;
        }
        String txnRef = returnSuccess
                ? blank(manual.getReturnSuccessTxnRef())
                : returnFailed ? blank(manual.getReturnFailedTxnRef()) : "";
        if (!txnRef.isBlank()) {
            DocxParagraphWalker.setParagraphText(paragraph, "Màn hình thông báo: (TxnRef: " + txnRef + ")");
            return true;
        }
        return false;
    }

    private boolean fillInstalmentParamLine(XWPFParagraph paragraph, String text, Map<String, String> params) {
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
        if (text.startsWith("Giao dịch đã được confirm")) {
            return Optional.of(TestCaseType.ORDER_ALREADY_CONFIRMED);
        }
        if (text.startsWith("Giao dịch không thành công")) {
            return Optional.of(TestCaseType.FAILED);
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
        return "2.1.1";
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}
