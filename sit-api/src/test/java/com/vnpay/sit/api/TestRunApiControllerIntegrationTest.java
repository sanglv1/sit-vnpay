package com.vnpay.sit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.api.dto.ApiResponse;
import com.vnpay.sit.auth.JwtAuthenticationFilter;
import com.vnpay.sit.auth.JwtService;
import com.vnpay.sit.auth.SitUserDetailsService;
import com.vnpay.sit.config.GlobalExceptionHandler;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.testrun.dto.TestRunForm;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.service.TestExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestRunApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class TestRunApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private TestExecutionService testExecutionService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SitUserDetailsService sitUserDetailsService;

    @Test
    void run_shouldReturnSuccessEnvelope() throws Exception {
        TestRun run = sampleRun();
        when(testExecutionService.execute(any(TestRunForm.class), any())).thenReturn(run);

        TestRunForm form = new TestRunForm();
        form.setPartnerId(1L);
        form.setCallbackType(CallbackType.IPN);
        form.setTestCase(TestCaseType.SUCCESS);
        form.setTxnRef("TXN001");
        form.setAmountVnd(100_000L);

        mockMvc.perform(post("/api/tests/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS))
                .andExpect(jsonPath("$.rspMsg").value("Success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.testCase").value("SUCCESS"));
    }

    @Test
    void run_wrongAmountWithoutOverride_shouldReturnBadRequest() throws Exception {
        TestRunForm form = new TestRunForm();
        form.setPartnerId(1L);
        form.setCallbackType(CallbackType.IPN);
        form.setTestCase(TestCaseType.WRONG_AMOUNT);
        form.setTxnRef("TXN002");
        form.setAmountVnd(100_000L);

        mockMvc.perform(post("/api/tests/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("01"))
                .andExpect(jsonPath("$.rspMsg").value("Nhập số tiền sai khi chọn test case WRONG_AMOUNT"));
    }

    @Test
    void metadata_shouldExposeTestCaseOptions() throws Exception {
        when(partnerService.findAllActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/tests/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS))
                .andExpect(jsonPath("$.data.testCases").isArray())
                .andExpect(jsonPath("$.data.testCases[0]['value']").value("INVALID_HASH"))
                .andExpect(jsonPath("$.data.callbackTypes[0]['value']").value("RETURN"))
                .andExpect(jsonPath("$.data.defaultTxnRef").exists());
    }

    @Test
    void detail_notFound_shouldReturnBadRequest() throws Exception {
        when(testExecutionService.findById(eq(999L), any())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/tests/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("01"))
                .andExpect(jsonPath("$.rspMsg").value("Không tìm thấy kết quả"));
    }

    private static TestRun sampleRun() {
        TestRun run = new TestRun();
        run.setId(10L);
        run.setPartnerId(1L);
        run.setPartnerName("Demo Merchant");
        run.setFlow(PaymentFlow.PAY);
        run.setCallbackType(CallbackType.IPN);
        run.setTestCase(TestCaseType.SUCCESS);
        run.setTxnRef("TXN001");
        run.setTargetUrl("http://merchant.test/ipn");
        run.setRequestParams("{\"vnp_TxnRef\":\"TXN001\"}");
        run.setHttpStatus(200);
        run.setResponseBody("{\"RspCode\":\"00\"}");
        run.setExpectedRspCode("00");
        run.setActualRspCode("00");
        run.setPassed(true);
        run.setDurationMs(20L);
        run.setCreatedAt(LocalDateTime.of(2026, 6, 15, 10, 0));
        return run;
    }
}
