package com.vnpay.sit.export;

import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class MinutesViewModelMapper {

    MinutesViewModel map(MinutesExportContext ctx) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("MERCHANT_NAME", blank(ctx.getPartner().getName()));
        tokens.put("TMN_CODE", blank(ctx.getPartner().getTmnCode()));
        tokens.put("MERCHANT_REPRESENTATIVE", ctx.resolvedMerchantRepresentative());
        tokens.put("VNPAY_REPRESENTATIVE", blank(ctx.getVnpayRepresentative()));
        tokens.put("SESSION_CREATOR_EMAIL", ctx.creatorEmail());

        tokens.put("CASE_1_RESULT", caseResult(ctx, TestCaseType.SUCCESS));
        tokens.put("CASE_2_RESULT", caseResult(ctx, TestCaseType.ORDER_ALREADY_CONFIRMED));
        tokens.put("CASE_3_RESULT", caseResult(ctx, TestCaseType.FAILED));
        tokens.put("CASE_4_RESULT", caseResult(ctx, TestCaseType.ORDER_NOT_FOUND));
        tokens.put("CASE_5_RESULT", caseResult(ctx, TestCaseType.WRONG_AMOUNT));
        tokens.put("CASE_6_RESULT", caseResult(ctx, TestCaseType.INVALID_HASH));

        ManualAcceptance manual = ctx.getManualAcceptance();
        tokens.put("MANUAL_EXCEPTION_FLAG", manualFlag(manual != null && Boolean.TRUE.equals(manual.getExceptionHandled())));
        tokens.put("MANUAL_WHITELIST_FLAG", manualFlag(manual != null && Boolean.TRUE.equals(manual.getWhitelistIpPassed())));
        tokens.put("MANUAL_LOG_FLAG", manualFlag(manual != null && Boolean.TRUE.equals(manual.getLogStoragePassed())));
        return new MinutesViewModel(tokens);
    }

    private String caseResult(MinutesExportContext ctx, TestCaseType testCase) {
        return ctx.run(testCase).map(TestRun::isPassed).map(this::evaluation).orElse("KHONG_CO_DU_LIEU");
    }

    private String evaluation(boolean passed) {
        return passed ? "DAT" : "KHONG_DAT";
    }

    private String manualFlag(boolean handled) {
        return handled ? "DA_XU_LY" : "CHUA_XU_LY";
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    record MinutesViewModel(Map<String, String> tokens) {
    }
}
