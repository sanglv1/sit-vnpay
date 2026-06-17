package com.vnpay.sit.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PayMinutesDocumentFiller {
    private static final Logger log = LoggerFactory.getLogger(PayMinutesDocumentFiller.class);

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final List<TestCaseType> AUTO_IPN_CASES = TestCaseType.ipnSuiteExecutionOrder();

    private final ObjectMapper objectMapper;
    private final MinutesViewModelMapper viewModelMapper;

    PayMinutesDocumentFiller(ObjectMapper objectMapper, MinutesViewModelMapper viewModelMapper) {
        this.objectMapper = objectMapper;
        this.viewModelMapper = viewModelMapper;
    }

    void fill(XWPFDocument document, MinutesExportContext ctx) {
        applyTemplateTokens(document, viewModelMapper.map(ctx));
        applyHeader(document, ctx);
        fillBody(document, ctx);
        fillConclusion(document, ctx);
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
            if (text.startsWith("Hà Nội ,ngày:")) {
                updated = dateText;
            } else if (text.startsWith("Tên merchant:")) {
                updated = "Tên merchant: " + ctx.getPartner().getName();
            } else if (text.startsWith("Mã định danh kết nối:")) {
                updated = "Mã định danh kết nối: " + ctx.getPartner().getTmnCode();
            } else if (text.startsWith("Dịch vụ kết nối:")) {
                updated = "Dịch vụ kết nối: PAY";
            } else if (text.startsWith("Môi trường kiểm tra:")) {
                updated = "Môi trường kiểm tra: SANDBOX";
            } else if (text.startsWith("Phiên bản tích hợp:")) {
                updated = "Phiên bản tích hợp: " + integrationVersion(ctx);
            } else if (text.startsWith("IPN URL test:")) {
                updated = "IPN URL test: " + ipnUrl(ctx);
            } else if (text.startsWith("Người thực hiện kiểm thử:")) {
                updated = "Người thực hiện kiểm thử: " + ctx.creatorEmail();
            } else if (text.startsWith("Đại diện merchant:")) {
                updated = "Đại diện merchant: " + ctx.resolvedMerchantRepresentative();
            }
            if (!updated.equals(text)) {
                DocxParagraphWalker.setParagraphText(paragraph, updated);
            }
        }
    }

    private void fillBody(XWPFDocument document, MinutesExportContext ctx) {
        boolean inReturnUrl = false;
        boolean inIpn = false;
        boolean returnSuccess = false;
        boolean returnFailed = false;
        TestCaseType currentCase = null;
        boolean inInput = false;
        boolean inOutput = false;
        boolean awaitingReturnEval = false;
        boolean awaitingIpnEval = false;
        String pendingManual = null;

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }

            if (text.contains("Return URL")) {
                inReturnUrl = true;
                inIpn = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
                pendingManual = null;
                continue;
            }
            if (text.contains("IPN URL")) {
                inIpn = true;
                inReturnUrl = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
                pendingManual = null;
                continue;
            }
            if (text.contains("Quy định khác")) {
                inIpn = false;
                inReturnUrl = false;
                currentCase = null;
                inInput = false;
                inOutput = false;
            }
            if (text.startsWith("Whitelist IP")) {
                pendingManual = "whitelist";
            } else if (text.startsWith("Lưu log giao dịch")) {
                pendingManual = "log";
            }

            if (inReturnUrl) {
                if (text.startsWith("Thanh toán thành công")) {
                    returnSuccess = true;
                    returnFailed = false;
                } else if (text.startsWith("Thanh toán không thành công")) {
                    returnFailed = true;
                    returnSuccess = false;
                }
            }

            if (inIpn) {
                currentCase = detectIpnCase(text).orElse(currentCase);
                if (text.startsWith("Lỗi ngoại lệ khác")) {
                    pendingManual = "exception";
                }
            }

            if (text.equalsIgnoreCase("Input:")) {
                inInput = true;
                inOutput = false;
                continue;
            }
            if (text.equalsIgnoreCase("Output:")) {
                inOutput = true;
                inInput = false;
                if (inReturnUrl) {
                    awaitingReturnEval = true;
                }
                if (inIpn && currentCase != null && currentCase != TestCaseType.UNKNOWN_ERROR) {
                    awaitingIpnEval = true;
                }
                continue;
            }

            if (inReturnUrl && inInput && text.startsWith("Mã giao dịch:")) {
                String txn = returnSuccess
                        ? txnRef(ctx.getManualAcceptance(), true)
                        : txnRef(ctx.getManualAcceptance(), false);
                DocxParagraphWalker.setParagraphText(paragraph, "Mã giao dịch: " + txn);
                continue;
            }

            if (inIpn && inInput && currentCase != null && currentCase != TestCaseType.UNKNOWN_ERROR) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (run.isPresent() && fillPayParamLine(paragraph, text, parseParams(run.get()))) {
                    continue;
                }
            }

            if (inIpn && inOutput && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (text.startsWith("RspCode:")) {
                    String rsp = currentCase == TestCaseType.UNKNOWN_ERROR
                            ? "\"99\""
                            : run.map(TestRun::getActualRspCode).map(this::blank).orElse("");
                    DocxParagraphWalker.setParagraphText(paragraph, "RspCode: " + rsp);
                    continue;
                }
                if (text.startsWith("Mô tả tình trạng:")) {
                    String message = run.map(TestRun::getResponseBody)
                            .map(this::extractMessage)
                            .filter(s -> !s.isBlank())
                            .orElse(defaultStatusMessage(currentCase));
                    DocxParagraphWalker.setParagraphText(paragraph, "Mô tả tình trạng: " + message);
                    continue;
                }
            }

            if (text.equals("ĐẠT") || text.equals("KHÔNG ĐẠT")) {
                if (awaitingReturnEval) {
                    DocxParagraphWalker.setParagraphText(paragraph,
                            evaluation(returnPassed(ctx, returnSuccess, returnFailed)));
                    awaitingReturnEval = false;
                    continue;
                }
                if (awaitingIpnEval && currentCase != null) {
                    DocxParagraphWalker.setParagraphText(paragraph,
                            evaluation(ctx.run(currentCase).map(TestRun::isPassed).orElse(false)));
                    awaitingIpnEval = false;
                    continue;
                }
            }

            if (text.equals("Đã xử lý") || text.equals("Chưa xử lý")) {
                if ("exception".equals(pendingManual)) {
                    DocxParagraphWalker.setParagraphText(paragraph,
                            manualHandled(ctx.getManualAcceptance() != null
                                    && Boolean.TRUE.equals(ctx.getManualAcceptance().getExceptionHandled())));
                    pendingManual = null;
                    continue;
                }
                if ("whitelist".equals(pendingManual)) {
                    DocxParagraphWalker.setParagraphText(paragraph,
                            manualHandled(ctx.getManualAcceptance() != null
                                    && Boolean.TRUE.equals(ctx.getManualAcceptance().getWhitelistIpPassed())));
                    pendingManual = null;
                    continue;
                }
                if ("log".equals(pendingManual)) {
                    DocxParagraphWalker.setParagraphText(paragraph,
                            manualHandled(ctx.getManualAcceptance() != null
                                    && Boolean.TRUE.equals(ctx.getManualAcceptance().getLogStoragePassed())));
                    pendingManual = null;
                }
            }
        }
    }

    private void fillConclusion(XWPFDocument document, MinutesExportContext ctx) {
        int autoTotal = AUTO_IPN_CASES.size();
        long autoPassed = AUTO_IPN_CASES.stream()
                .filter(type -> ctx.run(type).map(TestRun::isPassed).orElse(false))
                .count();
        int manualTotal = 5;
        int manualPassed = countManualPassed(ctx);
        String successRate = autoTotal == 0
                ? "0.00%"
                : String.format("%.2f%%", autoPassed * 100.0 / autoTotal);

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.startsWith("Tổng số kịch bản kiểm thử tự động:")) {
                DocxParagraphWalker.setParagraphText(paragraph,
                        "Tổng số kịch bản kiểm thử tự động: " + autoTotal);
            } else if (text.startsWith("Số kịch bản kiểm thử tự động đạt:")) {
                DocxParagraphWalker.setParagraphText(paragraph,
                        "Số kịch bản kiểm thử tự động đạt: " + autoPassed);
            } else if (text.startsWith("Tỉ lệ thành công:") || text.startsWith("Tỷ lệ thành công:")) {
                DocxParagraphWalker.setParagraphText(paragraph, "Tỉ lệ thành công: " + successRate);
            } else if (text.startsWith("Tổng số kịch bản kiểm thử thủ công:")) {
                DocxParagraphWalker.setParagraphText(paragraph,
                        "Tổng số kịch bản kiểm thử thủ công: " + manualTotal);
            } else if (text.startsWith("Số kịch bản đạt:")) {
                DocxParagraphWalker.setParagraphText(paragraph, "Số kịch bản đạt: " + manualPassed);
            }
        }
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
            if (text.startsWith("ĐẠI DIỆN ")) {
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

    private int countManualPassed(MinutesExportContext ctx) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return 0;
        }
        int count = 0;
        if (hasText(manual.getReturnSuccessTxnRef())) {
            count++;
        }
        if (hasText(manual.getReturnFailedTxnRef())) {
            count++;
        }
        if (Boolean.TRUE.equals(manual.getExceptionHandled())) {
            count++;
        }
        if (Boolean.TRUE.equals(manual.getWhitelistIpPassed())) {
            count++;
        }
        if (Boolean.TRUE.equals(manual.getLogStoragePassed())) {
            count++;
        }
        return count;
    }

    private boolean returnPassed(MinutesExportContext ctx, boolean returnSuccess, boolean returnFailed) {
        ManualAcceptance manual = ctx.getManualAcceptance();
        if (manual == null) {
            return false;
        }
        if (returnSuccess) {
            return hasText(manual.getReturnSuccessTxnRef());
        }
        if (returnFailed) {
            return hasText(manual.getReturnFailedTxnRef());
        }
        return false;
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

    private boolean fillPayParamLine(XWPFParagraph paragraph, String text, Map<String, String> params) {
        int colon = text.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String key = text.substring(0, colon).trim();
        if (!key.startsWith("vnp_")) {
            return false;
        }
        DocxParagraphWalker.setParagraphText(paragraph, key + ": " + params.getOrDefault(key, ""));
        return true;
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
            case SUCCESS -> "Confirm Success";
            case ORDER_ALREADY_CONFIRMED -> "Order already confirmed";
            case FAILED -> "Pending bank status";
            case ORDER_NOT_FOUND -> "Order not found";
            case WRONG_AMOUNT -> "Invalid amount";
            case INVALID_HASH -> "Invalid signature";
            case UNKNOWN_ERROR -> "Unknown error";
        };
    }

    private String txnRef(ManualAcceptance manual, boolean success) {
        if (manual == null) {
            return "";
        }
        return success
                ? blank(manual.getReturnSuccessTxnRef())
                : blank(manual.getReturnFailedTxnRef());
    }

    private String ipnUrl(MinutesExportContext ctx) {
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

    private String evaluation(boolean passed) {
        return passed ? "ĐẠT" : "KHÔNG ĐẠT";
    }

    private String manualHandled(boolean handled) {
        return handled ? "Đã xử lý" : "Chưa xử lý";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}
