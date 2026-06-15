package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.*;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.testrun.dto.PrepareMerchantOrderForm;
import com.vnpay.sit.testrun.dto.TestRunForm;
import com.vnpay.sit.testrun.dto.TestSuiteForm;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.service.TestExecutionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tests")
public class TestRunApiController {

    private final PartnerService partnerService;
    private final TestExecutionService testExecutionService;

    public TestRunApiController(PartnerService partnerService, TestExecutionService testExecutionService) {
        this.partnerService = partnerService;
        this.testExecutionService = testExecutionService;
    }

    @GetMapping("/metadata")
    public ApiResponse<TestMetadataResponse> metadata() {
        TestMetadataResponse data = TestMetadataResponse.builder()
                .partners(partnerService.findAllActive().stream()
                        .map(p -> PartnerResponse.from(p, false))
                        .toList())
                .callbackTypes(toOptions(CallbackType.values()))
                .testCases(toTestCaseOptions())
                .paymentFlows(toOptions(PaymentFlow.values()))
                .defaultTxnRef("SIT" + System.currentTimeMillis() % 1_000_000)
                .build();
        return ApiResponse.ok(data);
    }

    @PostMapping("/run")
    public ApiResponse<TestRunResponse> run(
            @Valid @RequestBody TestRunForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        if (form.getTestCase() == TestCaseType.WRONG_AMOUNT && form.getWrongAmountVnd() == null) {
            throw new IllegalArgumentException("Nhập số tiền sai khi chọn test case WRONG_AMOUNT");
        }
        TestRun result = testExecutionService.execute(form, principal);
        return ApiResponse.ok(TestRunResponse.from(result));
    }

    @PostMapping("/run-ipn-suite")
    public ApiResponse<TestSuiteResponse> runIpnSuite(
            @Valid @RequestBody TestSuiteForm form,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        TestSuiteResponse result = testExecutionService.executeIpnSuite(form, principal);
        return ApiResponse.ok(result);
    }

    @PostMapping("/prepare-merchant-order")
    public ApiResponse<PrepareOrderResponse> prepareMerchantOrder(@Valid @RequestBody PrepareMerchantOrderForm form) {
        return ApiResponse.ok(testExecutionService.prepareMerchantOrder(form));
    }

    @GetMapping
    public ApiResponse<PageResponse<TestRunResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long sessionId,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        Page<TestRun> runs = testExecutionService.findHistory(sessionId, PageRequest.of(page, size), principal);
        PageResponse<TestRunResponse> data = new PageResponse<>(
                runs.getContent().stream().map(TestRunResponse::from).toList(),
                runs.getNumber(),
                runs.getSize(),
                runs.getTotalElements(),
                runs.getTotalPages()
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<TestRunResponse> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal SitUserPrincipal principal
    ) {
        TestRun run = testExecutionService.findById(id, principal)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả"));
        return ApiResponse.ok(TestRunResponse.from(run));
    }

    private static java.util.List<EnumOption> toOptions(PaymentFlow[] values) {
        return java.util.Arrays.stream(values)
                .map(v -> new EnumOption(v.name(), v.getLabel(), null, null, 0))
                .toList();
    }

    private static java.util.List<EnumOption> toOptions(CallbackType[] values) {
        return java.util.Arrays.stream(values)
                .map(v -> new EnumOption(v.name(), v.getLabel(), null, null, 0))
                .toList();
    }

    private static java.util.List<EnumOption> toTestCaseOptions() {
        return TestCaseType.autoIpnTestCases().stream()
                .map(v -> new EnumOption(v.name(), v.getLabel(), v.getExpectedRspCode(),
                        v.getCaseCode(), v.getCheckOrder()))
                .toList();
    }
}
