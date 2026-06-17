package com.vnpay.sit.api;

import com.vnpay.sit.api.dto.TestSessionResponse;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.JwtAuthenticationFilter;
import com.vnpay.sit.auth.JwtService;
import com.vnpay.sit.auth.SitUserDetailsService;
import com.vnpay.sit.config.GlobalExceptionHandler;
import com.vnpay.sit.export.MinutesExportService;
import com.vnpay.sit.session.service.TestSessionService;
import com.vnpay.sit.testrun.service.TestExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestSessionApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class TestSessionApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestSessionService testSessionService;

    @MockBean
    private MinutesExportService minutesExportService;

    @MockBean
    private TestExecutionService testExecutionService;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SitUserDetailsService sitUserDetailsService;

    @Test
    void list_shouldKeepSessionResponseContract() throws Exception {
        TestSessionResponse item = TestSessionResponse.builder()
                .id(10L)
                .partnerId(1L)
                .partnerName("Demo Merchant")
                .tmnCode("TMN001")
                .note("note")
                .status("OPEN")
                .autoPassed(3)
                .autoTotal(6)
                .pendingTxnRef("TXN001")
                .pendingAmountVnd(100_000L)
                .confirmedTxnRef("TXN001")
                .confirmedAmountVnd(100_000L)
                .wrongAmountVnd(101_000L)
                .createdByEmail("merchant@shop.com")
                .createdAt(LocalDateTime.of(2026, 6, 16, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 16, 8, 5))
                .build();
        PageImpl<TestSessionResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        org.mockito.Mockito.when(testSessionService.findAll(any(), eq(null), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].partnerId").value(1))
                .andExpect(jsonPath("$.data.content[0].partnerName").value("Demo Merchant"))
                .andExpect(jsonPath("$.data.content[0].tmnCode").value("TMN001"))
                .andExpect(jsonPath("$.data.content[0].autoPassed").value(3))
                .andExpect(jsonPath("$.data.content[0].autoTotal").value(6))
                .andExpect(jsonPath("$.data.content[0].pendingTxnRef").value("TXN001"))
                .andExpect(jsonPath("$.data.content[0].pendingAmountVnd").value(100000))
                .andExpect(jsonPath("$.data.content[0].confirmedTxnRef").value("TXN001"))
                .andExpect(jsonPath("$.data.content[0].wrongAmountVnd").value(101000))
                .andExpect(jsonPath("$.data.content[0].createdByEmail").value("merchant@shop.com"))
                .andExpect(jsonPath("$.data.content[0].createdAt").exists())
                .andExpect(jsonPath("$.data.content[0].updatedAt").exists());
    }

    @Test
    void workspace_shouldKeepWorkspaceContract() throws Exception {
        TestSessionResponse session = TestSessionResponse.builder()
                .id(10L)
                .partnerId(1L)
                .partnerName("Demo Merchant")
                .tmnCode("TMN001")
                .status("OPEN")
                .autoPassed(2)
                .autoTotal(6)
                .build();
        org.mockito.Mockito.when(testSessionService.getById(eq(10L), any())).thenReturn(session);
        org.mockito.Mockito.when(testExecutionService.findLatestRunsForSession(eq(10L), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/sessions/10/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session.id").value(10))
                .andExpect(jsonPath("$.data.latestRuns").isArray())
                .andExpect(jsonPath("$.data.testCases").isArray())
                .andExpect(jsonPath("$.data.testCases[0].value").exists());
    }
}
