package com.vnpay.sit.api;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.JwtAuthenticationFilter;
import com.vnpay.sit.auth.JwtService;
import com.vnpay.sit.auth.SitUserDetailsService;
import com.vnpay.sit.config.GlobalExceptionHandler;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class PartnerApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SitUserDetailsService sitUserDetailsService;

    @Test
    void list_shouldKeepPartnerResponseContract() throws Exception {
        PartnerConfig partner = new PartnerConfig();
        partner.setId(1L);
        partner.setName("Demo Merchant");
        partner.setFlow(PaymentFlow.PAY);
        partner.setTmnCode("TMN001");
        partner.setSecretKey("SECRET_KEY");
        partner.setReturnUrl("http://merchant/return");
        partner.setIpnUrl("http://merchant/ipn");
        partner.setNote("note");
        partner.setActive(true);
        partner.setCreatedByEmail("merchant@shop.com");
        partner.setCreatedAt(LocalDateTime.of(2026, 6, 16, 8, 0));
        partner.setUpdatedAt(LocalDateTime.of(2026, 6, 16, 8, 5));

        org.mockito.Mockito.when(partnerService.findAllForPrincipal(any())).thenReturn(List.of(partner));
        org.mockito.Mockito.when(accessControlService.canViewPartnerSecret(any(), any())).thenReturn(true);

        mockMvc.perform(get("/api/partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Demo Merchant"))
                .andExpect(jsonPath("$.data[0].flow").value("PAY"))
                .andExpect(jsonPath("$.data[0].flowLabel").exists())
                .andExpect(jsonPath("$.data[0].tmnCode").value("TMN001"))
                .andExpect(jsonPath("$.data[0].secretKey").exists())
                .andExpect(jsonPath("$.data[0].returnUrl").exists())
                .andExpect(jsonPath("$.data[0].ipnUrl").exists())
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].createdByEmail").value("merchant@shop.com"))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].updatedAt").exists());
    }
}
