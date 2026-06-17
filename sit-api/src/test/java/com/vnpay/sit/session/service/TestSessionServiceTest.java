package com.vnpay.sit.session.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.session.SessionCompletionFilter;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestSessionServiceTest {

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private PartnerService partnerService;

    @Mock
    private AccessControlService accessControlService;

    private TestSessionService testSessionService;

    @BeforeEach
    void setUp() {
        testSessionService = new TestSessionService(
                sessionRepository,
                testRunRepository,
                partnerService,
                accessControlService
        );
    }

    @Test
    void findAll_adminShouldNotFilterByEmail() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(accessControlService.isAdmin(any())).thenReturn(true);
        when(sessionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty(pageable));

        testSessionService.findAll(pageable, null, SessionCompletionFilter.ALL, principal("admin@vnpay.vn", UserRole.ADMIN));

        verify(sessionRepository).findAll(any(Specification.class), any(PageRequest.class));
        verify(accessControlService, never()).currentUserEmail(any());
    }

    @Test
    void findAll_merchantShouldFilterByOwnerEmail() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(accessControlService.isAdmin(any())).thenReturn(false);
        when(accessControlService.currentUserEmail(any())).thenReturn("merchant.qc@shop.com");
        when(sessionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty(pageable));

        testSessionService.findAll(pageable, null, SessionCompletionFilter.ALL, principal("merchant.qc@shop.com", UserRole.MERCHANT_QC));

        verify(accessControlService).currentUserEmail(any());
        verify(sessionRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    private static SitUserPrincipal principal(String email, UserRole role) {
        SitUser user = new SitUser();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);
        return new SitUserPrincipal(user);
    }
}
