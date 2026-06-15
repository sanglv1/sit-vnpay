package com.vnpay.sit.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class MinutesDocumentFiller {

    private static final String LOG_PLACEHOLDER = "[Merchant điền log request sang VNPAY tại đây]";
    private static final String IMAGE_PLACEHOLDER = "[Merchant chụp và dán ảnh tại đây]";
    private static final String PARAM_PLACEHOLDER = "[Nhập giá trị của tham số]";

    private final ObjectMapper objectMapper;

    MinutesDocumentFiller(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void fill(XWPFDocument document, MinutesExportContext ctx) {
        applyHeader(document, ctx);
        fillBody(document, ctx);
    }

    private void applyHeader(XWPFDocument document, MinutesExportContext ctx) {
        var date = ctx.exportDate();
        String dateText = "Hà Nội ,ngày: " + date.getDayOfMonth()
                + " tháng: " + date.getMonthValue()
                + " năm: " + date.getYear();

        Map<String, String> replacements = Map.of(
                "Hà Nội ,ngày: … tháng: …năm:2025", dateText,
                "Tên merchant:", "Tên merchant: " + ctx.getPartner().getName(),
                "Mã định danh kết nối:", "Mã định danh kết nối: " + ctx.getPartner().getTmnCode(),
                "Tên website/app kết nối:", "Tên website/app kết nối: " + websiteLabel(ctx),
                "Link test:", "Link test: " + testLink(ctx),
                "Phiên bản tích hợp: 2.1.0", "Phiên bản tích hợp: " + integrationVersion(ctx),
                "Phiên bản tích hợp: 2.1.1", "Phiên bản tích hợp: " + integrationVersion(ctx),
                "Đại diện VNPAY:", "Đại diện VNPAY: " + blank(ctx.getVnpayRepresentative()),
                "Đại diện merchant:", "Đại diện merchant: " + merchantRep(ctx)
        );

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String updated = text;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                if (updated.contains(entry.getKey())) {
                    updated = updated.replace(entry.getKey(), entry.getValue());
                }
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
        boolean outputSection = false;
        String pendingSignoff = null;

        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }

            if (text.contains("Return URL")) {
                inReturnUrl = true;
                inIpn = false;
                currentCase = null;
                outputSection = false;
                pendingSignoff = null;
                continue;
            }
            if (text.contains("IPN URL")) {
                inIpn = true;
                inReturnUrl = false;
                currentCase = null;
                outputSection = false;
                pendingSignoff = null;
                continue;
            }
            if (text.contains("Quy định khác")) {
                inIpn = false;
                inReturnUrl = false;
                currentCase = null;
                outputSection = false;
            }
            if (text.startsWith("Whitelist IP")) {
                pendingSignoff = "whitelist";
            } else if (text.startsWith("Lưu log giao dịch")) {
                pendingSignoff = "log";
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
                if (text.equalsIgnoreCase("Output:")) {
                    outputSection = true;
                }
                if (text.startsWith("Input:")) {
                    outputSection = false;
                }
            }

            if (text.contains(LOG_PLACEHOLDER)) {
                DocxParagraphWalker.setParagraphText(paragraph, text.replace(LOG_PLACEHOLDER, logContent(ctx, inIpn, currentCase, returnSuccess, returnFailed)));
                continue;
            }
            if (text.contains(IMAGE_PLACEHOLDER)) {
                DocxParagraphWalker.setParagraphText(paragraph, text.replace(IMAGE_PLACEHOLDER, imageContent(ctx, returnSuccess, returnFailed)));
                continue;
            }
            if (text.contains(PARAM_PLACEHOLDER)) {
                DocxParagraphWalker.setParagraphText(paragraph, replaceNextParamPlaceholder(text, ctx, currentCase));
                continue;
            }

            if (inIpn && currentCase != null) {
                Optional<TestRun> run = ctx.run(currentCase);
                if (run.isPresent()) {
                    Map<String, String> params = parseParams(run.get());
                    if (fillParamLine(paragraph, text, params, ctx.flow())) {
                        continue;
                    }
                    if (outputSection && text.startsWith("Message:")) {
                        DocxParagraphWalker.setParagraphText(paragraph,
                                text + " | Đánh giá: " + ctx.evaluation(run.get().isPassed()));
                        outputSection = false;
                    }
                }
            }

            if (text.startsWith("Đại diện merchant ký xác nhận tại đây:")) {
                String signoff = signoffEvaluation(text, ctx, pendingSignoff);
                DocxParagraphWalker.setParagraphText(paragraph, signoff);
                pendingSignoff = null;
            }
        }
    }

    private Optional<TestCaseType> detectIpnCase(String text) {
        if (text.startsWith("Giao dịch thành công")) {
            return Optional.of(TestCaseType.SUCCESS);
        }
        if (text.startsWith("Giao dịch thất bại")) {
            return Optional.of(TestCaseType.FAILED);
        }
        if (text.startsWith("Không tìm thấy giao dịch confirm")) {
            return Optional.of(TestCaseType.ORDER_NOT_FOUND);
        }
        if (text.startsWith("Giao dịch đã được confirm")) {
            return Optional.of(TestCaseType.ORDER_ALREADY_CONFIRMED);
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

    private boolean fillParamLine(XWPFParagraph paragraph, String text, Map<String, String> params, PaymentFlow flow) {
        if (text.startsWith("vnp_TxnRef (order.orderReference):")) {
            DocxParagraphWalker.setParagraphText(paragraph,
                    "vnp_TxnRef (order.orderReference): " + txnRef(params, flow));
            return true;
        }

        String[] keys = flow == PaymentFlow.PAY
                ? new String[]{"vnp_TxnRef", "vnp_TmnCode", "vnp_Amount", "vnp_TransactionNo",
                "vnp_ResponseCode", "vnp_TransactionStatus", "vnp_PayDate", "vnp_SecureHash"}
                : new String[]{"vnp_txn_ref", "vnp_tmn_code", "vnp_amount", "vnp_transaction_no",
                "vnp_response_code", "vnp_transaction_status", "vnp_pay_date", "vnp_secure_hash"};

        for (String key : keys) {
            String prefix = key + ":";
            if (text.equals(prefix) || text.equals(prefix.trim())) {
                DocxParagraphWalker.setParagraphText(paragraph, prefix + " " + params.getOrDefault(key, ""));
                return true;
            }
        }
        return false;
    }

    private String replaceNextParamPlaceholder(String text, MinutesExportContext ctx, TestCaseType currentCase) {
        if (currentCase == null) {
            return text.replace(PARAM_PLACEHOLDER, "");
        }
        return ctx.run(currentCase)
                .map(run -> text.replace(PARAM_PLACEHOLDER, firstParamValue(parseParams(run), ctx.flow())))
                .orElse(text.replace(PARAM_PLACEHOLDER, ""));
    }

    private String logContent(
            MinutesExportContext ctx,
            boolean inIpn,
            TestCaseType currentCase,
            boolean returnSuccess,
            boolean returnFailed
    ) {
        if (inIpn && currentCase != null) {
            return ctx.run(currentCase)
                    .map(TestRun::getRequestParams)
                    .map(this::compactJson)
                    .orElse("");
        }
        if (returnSuccess && ctx.getManualAcceptance() != null) {
            return "TxnRef: " + blank(ctx.getManualAcceptance().getReturnSuccessTxnRef());
        }
        if (returnFailed && ctx.getManualAcceptance() != null) {
            return "TxnRef: " + blank(ctx.getManualAcceptance().getReturnFailedTxnRef());
        }
        return "Phiên SIT #" + ctx.getSession().getId();
    }

    private String imageContent(MinutesExportContext ctx, boolean returnSuccess, boolean returnFailed) {
        if (returnSuccess && ctx.getManualAcceptance() != null) {
            String txn = blank(ctx.getManualAcceptance().getReturnSuccessTxnRef());
            boolean hasImage = ctx.getManualAcceptance().getReturnSuccessImage() != null
                    && !ctx.getManualAcceptance().getReturnSuccessImage().isBlank();
            return hasImage
                    ? "Đã nộp ảnh Return URL thành công (TxnRef: " + txn + ")"
                    : "TxnRef: " + txn;
        }
        if (returnFailed && ctx.getManualAcceptance() != null) {
            String txn = blank(ctx.getManualAcceptance().getReturnFailedTxnRef());
            boolean hasImage = ctx.getManualAcceptance().getReturnFailedImage() != null
                    && !ctx.getManualAcceptance().getReturnFailedImage().isBlank();
            return hasImage
                    ? "Đã nộp ảnh Return URL thất bại (TxnRef: " + txn + ")"
                    : "TxnRef: " + txn;
        }
        return IMAGE_PLACEHOLDER;
    }

    private String signoffEvaluation(String text, MinutesExportContext ctx, String pendingSignoff) {
        if (ctx.getManualAcceptance() == null || pendingSignoff == null) {
            return text;
        }
        if ("whitelist".equals(pendingSignoff) && ctx.getManualAcceptance().getWhitelistIpPassed() != null) {
            return text + " " + ctx.evaluation(ctx.getManualAcceptance().getWhitelistIpPassed());
        }
        if ("log".equals(pendingSignoff) && ctx.getManualAcceptance().getLogStoragePassed() != null) {
            return text + " " + ctx.evaluation(ctx.getManualAcceptance().getLogStoragePassed());
        }
        return text;
    }

    private String firstParamValue(Map<String, String> params, PaymentFlow flow) {
        for (String key : orderedParamKeys(flow)) {
            String value = params.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private List<String> orderedParamKeys(PaymentFlow flow) {
        if (flow == PaymentFlow.PAY) {
            return List.of("vnp_TxnRef", "vnp_TmnCode", "vnp_TransactionNo", "vnp_PayDate");
        }
        return List.of("vnp_txn_ref", "vnp_tmn_code", "vnp_transaction_no", "vnp_pay_date");
    }

    private Map<String, String> parseParams(TestRun run) {
        try {
            return objectMapper.readValue(run.getRequestParams(), new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String txnRef(Map<String, String> params, PaymentFlow flow) {
        return flow == PaymentFlow.PAY
                ? params.getOrDefault("vnp_TxnRef", "")
                : params.getOrDefault("vnp_txn_ref", "");
    }

    private String compactJson(String json) {
        if (json == null) {
            return "";
        }
        return json.replaceAll("\\s+", " ").trim();
    }

    private String websiteLabel(MinutesExportContext ctx) {
        if (ctx.getWebsiteName() != null && !ctx.getWebsiteName().isBlank()) {
            return ctx.getWebsiteName().trim();
        }
        return ctx.getPartner().getReturnUrl();
    }

    private String testLink(MinutesExportContext ctx) {
        if (ctx.getTestLink() != null && !ctx.getTestLink().isBlank()) {
            return ctx.getTestLink().trim();
        }
        return ctx.getPartner().getReturnUrl();
    }

    private String integrationVersion(MinutesExportContext ctx) {
        if (ctx.getIntegrationVersion() != null && !ctx.getIntegrationVersion().isBlank()) {
            return ctx.getIntegrationVersion().trim();
        }
        return ctx.flow() == PaymentFlow.PAY || ctx.flow() == PaymentFlow.TOKEN ? "2.1.0" : "2.1.1";
    }

    private String merchantRep(MinutesExportContext ctx) {
        if (ctx.getMerchantRepresentative() != null && !ctx.getMerchantRepresentative().isBlank()) {
            return ctx.getMerchantRepresentative().trim();
        }
        return blank(ctx.getSession().getCreatedByEmail());
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}
