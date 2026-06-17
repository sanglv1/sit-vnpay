package com.vnpay.sit.auth;

import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private TestSessionRepository sessionRepository;

    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(sessionRepository);
    }

    @Test
    void requireSessionAccess_shouldAllowOwner() {
        TestSession session = new TestSession();
        session.setId(1L);
        session.setCreatedByEmail("qc@merchant.com");

        assertThatCode(() -> accessControlService.requireSessionAccess(
                session, principal("qc@merchant.com", UserRole.MERCHANT_QC)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireSessionAccess_shouldDenyOtherMerchant() {
        TestSession session = new TestSession();
        session.setId(1L);
        session.setCreatedByEmail("owner@merchant.com");

        assertThatThrownBy(() -> accessControlService.requireSessionAccess(
                session, principal("other@merchant.com", UserRole.MERCHANT_QC)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void requireSessionAccess_shouldAllowAdminRegardlessOfOwner() {
        TestSession session = new TestSession();
        session.setId(1L);
        session.setCreatedByEmail("owner@merchant.com");

        assertThatCode(() -> accessControlService.requireSessionAccess(
                session, principal("admin@vnpay.vn", UserRole.ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireSessionAccess_byId_shouldLoadSession() {
        TestSession session = new TestSession();
        session.setId(5L);
        session.setCreatedByEmail("qc@merchant.com");
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));

        assertThatCode(() -> accessControlService.requireSessionAccess(
                5L, principal("qc@merchant.com", UserRole.MERCHANT_QC)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireTestRunAccess_shouldCheckSessionOwnership() {
        TestRun run = new TestRun();
        run.setId(10L);
        run.setSessionId(3L);

        TestSession session = new TestSession();
        session.setId(3L);
        session.setCreatedByEmail("qc@merchant.com");
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatCode(() -> accessControlService.requireTestRunAccess(
                run, principal("qc@merchant.com", UserRole.MERCHANT_QC)))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePartnerAccess_shouldAllowCreator() {
        PartnerConfig partner = new PartnerConfig();
        partner.setId(1L);
        partner.setCreatedByEmail("qc@merchant.com");

        assertThatCode(() -> accessControlService.requirePartnerAccess(
                partner, principal("qc@merchant.com", UserRole.MERCHANT_QC)))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePartnerAccess_shouldDenyOtherMerchant() {
        PartnerConfig partner = new PartnerConfig();
        partner.setId(1L);
        partner.setCreatedByEmail("creator@merchant.com");

        assertThatThrownBy(() -> accessControlService.requirePartnerAccess(
                partner, principal("other@merchant.com", UserRole.MERCHANT_QC)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void requirePartnerAccess_shouldAllowAdmin() {
        PartnerConfig partner = new PartnerConfig();
        partner.setId(1L);
        partner.setCreatedByEmail("creator@merchant.com");

        assertThatCode(() -> accessControlService.requirePartnerAccess(
                partner, principal("admin@vnpay.vn", UserRole.ADMIN)))
                .doesNotThrowAnyException();
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
