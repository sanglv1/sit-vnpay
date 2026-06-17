package com.vnpay.sit.partner.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerConfigRepository repository;

    @Mock
    private AccessControlService accessControlService;

    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        partnerService = new PartnerService(repository, accessControlService);
    }

    @Test
    void findAllForPrincipal_adminShouldSeeAllPartners() {
        List<PartnerConfig> expected = List.of(new PartnerConfig(), new PartnerConfig());
        when(accessControlService.isAdmin(any())).thenReturn(true);
        when(repository.findAll()).thenReturn(expected);

        List<PartnerConfig> actual = partnerService.findAllForPrincipal(principal("admin@vnpay.vn", UserRole.ADMIN));

        assertThat(actual).isEqualTo(expected);
        verify(repository).findAll();
        verify(repository, never()).findByCreatedByEmailIgnoreCaseOrderByNameAsc(any());
    }

    @Test
    void findAllForPrincipal_merchantShouldSeeOwnPartnersOnly() {
        List<PartnerConfig> expected = List.of(new PartnerConfig());
        when(accessControlService.isAdmin(any())).thenReturn(false);
        when(accessControlService.currentUserEmail(any())).thenReturn("merchant.qc@shop.com");
        when(repository.findByCreatedByEmailIgnoreCaseOrderByNameAsc("merchant.qc@shop.com"))
                .thenReturn(expected);

        List<PartnerConfig> actual = partnerService.findAllForPrincipal(
                principal("merchant.qc@shop.com", UserRole.MERCHANT_QC));

        assertThat(actual).isEqualTo(expected);
        verify(repository).findByCreatedByEmailIgnoreCaseOrderByNameAsc("merchant.qc@shop.com");
        verify(repository, never()).findAll();
    }

    @Test
    void findAllActiveForPrincipal_merchantShouldSeeOwnActivePartnersOnly() {
        List<PartnerConfig> expected = List.of(new PartnerConfig());
        when(accessControlService.isAdmin(any())).thenReturn(false);
        when(accessControlService.currentUserEmail(any())).thenReturn("merchant.qc@shop.com");
        when(repository.findByActiveTrueAndCreatedByEmailIgnoreCaseOrderByNameAsc("merchant.qc@shop.com"))
                .thenReturn(expected);

        List<PartnerConfig> actual = partnerService.findAllActiveForPrincipal(
                principal("merchant.qc@shop.com", UserRole.MERCHANT_QC));

        assertThat(actual).isEqualTo(expected);
        verify(repository).findByActiveTrueAndCreatedByEmailIgnoreCaseOrderByNameAsc("merchant.qc@shop.com");
        verify(repository, never()).findByActiveTrueOrderByNameAsc();
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
