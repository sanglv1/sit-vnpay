package com.vnpay.sit.export;

import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.testrun.entity.TestRun;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
public class MinutesExportContext {

    private final TestSession session;
    private final PartnerConfig partner;
    private final ManualAcceptance manualAcceptance;
    private final Map<TestCaseType, TestRun> ipnRuns;
    private final String vnpayRepresentative;
    private final String merchantRepresentative;
    private final String websiteName;
    private final String testLink;
    private final String integrationVersion;

    public PaymentFlow flow() {
        return partner.getFlow();
    }

    public Optional<TestRun> run(TestCaseType testCase) {
        return Optional.ofNullable(ipnRuns.get(testCase));
    }

    public static Map<TestCaseType, TestRun> indexLatestIpnRuns(java.util.List<TestRun> runs) {
        return com.vnpay.sit.testrun.service.TestRunGrouping.effectiveByTestCase(
                runs,
                run -> run.getTestCase() != null
        );
    }

    public LocalDate exportDate() {
        return session.getCreatedAt() != null
                ? session.getCreatedAt().toLocalDate()
                : LocalDate.now();
    }

    public String evaluation(boolean passed) {
        return passed ? "Đạt" : "Không đạt";
    }

    /** Email tài khoản tạo phiên (MERCHANT_QC / ADMIN đang đăng nhập lúc tạo). */
    public String creatorEmail() {
        return session.getCreatedByEmail() != null ? session.getCreatedByEmail().trim() : "";
    }

    public String resolvedMerchantRepresentative() {
        return creatorEmail();
    }
}
