package com.vnpay.sit.manual.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.manual.dto.ManualAcceptanceForm;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.repository.ManualAcceptanceRepository;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualAcceptanceServiceTest {

  @Mock private ManualAcceptanceRepository repository;
  @Mock private PartnerService partnerService;
  @Mock private AccessControlService accessControlService;

  private ManualAcceptanceService manualAcceptanceService;

  @BeforeEach
  void setUp() {
    manualAcceptanceService =
        new ManualAcceptanceService(repository, partnerService, accessControlService);
  }

  @Test
  void save_withSessionShouldPersistQcFields() {
    PartnerConfig partner = partner(1L, "Merchant A");
    TestSession session = session(5L, 1L);
    ManualAcceptanceForm form = form(1L, 5L);
    form.setReturnSuccessTxnRef("  OK_REF  ");
    form.setReturnFailedTxnRef("FAIL_REF");
    form.setExceptionHandled(true);
    form.setWhitelistIpPassed(true);
    form.setLogStoragePassed(false);
    form.setNote("QC note");

    SitUserPrincipal principal = principal("qc@merchant.com", UserRole.MERCHANT_QC);
    when(partnerService.requireAccessible(1L, principal)).thenReturn(partner);
    when(accessControlService.requireSession(5L)).thenReturn(session);
    doNothing().when(accessControlService).requireSessionAccess(session, principal);
    doNothing().when(accessControlService).requireAdmin(principal);
    when(repository.findTopBySessionIdOrderByUpdatedAtDesc(5L))
        .thenReturn(Optional.empty());
    when(repository.save(any(ManualAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ManualAcceptance saved = manualAcceptanceService.save(form, principal);

    assertThat(saved.getPartnerId()).isEqualTo(1L);
    assertThat(saved.getSessionId()).isEqualTo(5L);
    assertThat(saved.getPartnerName()).isEqualTo("Merchant A");
    assertThat(saved.getReturnSuccessTxnRef()).isEqualTo("OK_REF");
    assertThat(saved.getReturnFailedTxnRef()).isEqualTo("FAIL_REF");
    assertThat(saved.getExceptionHandled()).isTrue();
    assertThat(saved.getWhitelistIpPassed()).isTrue();
    assertThat(saved.getLogStoragePassed()).isFalse();
    assertThat(saved.getNote()).isEqualTo("QC note");
    verify(accessControlService).requireSessionAccess(session, principal);
    verify(accessControlService).requireAdmin(principal);
  }

  @Test
  void save_shouldRejectMismatchedPartnerAndSession() {
    PartnerConfig partner = partner(1L, "Merchant A");
    TestSession session = session(5L, 99L);
    ManualAcceptanceForm form = form(1L, 5L);

    SitUserPrincipal principal = principal("qc@merchant.com", UserRole.MERCHANT_QC);
    when(partnerService.requireAccessible(1L, principal)).thenReturn(partner);
    when(accessControlService.requireSession(5L)).thenReturn(session);

    assertThatThrownBy(() -> manualAcceptanceService.save(form, principal))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Terminal không khớp");
    verify(repository, never()).save(any());
  }

  @Test
  void save_updateExistingShouldRequireSessionAccess() {
    PartnerConfig partner = partner(1L, "Merchant A");
    TestSession session = session(5L, 1L);
    ManualAcceptance existing = new ManualAcceptance();
    existing.setId(20L);
    existing.setSessionId(5L);

    ManualAcceptanceForm form = form(1L, 5L);
    form.setId(20L);
    form.setExceptionHandled(true);

    SitUserPrincipal principal = principal("qc@merchant.com", UserRole.MERCHANT_QC);
    when(partnerService.requireAccessible(1L, principal)).thenReturn(partner);
    when(accessControlService.requireSession(5L)).thenReturn(session);
    doNothing().when(accessControlService).requireSessionAccess(session, principal);
    when(repository.findById(20L)).thenReturn(Optional.of(existing));
    doNothing().when(accessControlService).requireSessionAccess(5L, principal);
    when(repository.save(any(ManualAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ManualAcceptance saved = manualAcceptanceService.save(form, principal);

    assertThat(saved.getId()).isEqualTo(20L);
    assertThat(saved.getExceptionHandled()).isTrue();
    verify(accessControlService, org.mockito.Mockito.times(2))
        .requireSessionAccess(5L, principal);
  }

  @Test
  void save_withoutSessionShouldRequireAdmin() {
    PartnerConfig partner = partner(1L, "Merchant A");
    ManualAcceptanceForm form = form(1L, null);

    SitUserPrincipal principal = principal("admin@vnpay.vn", UserRole.ADMIN);
    when(partnerService.requireAccessible(1L, principal)).thenReturn(partner);
    doNothing().when(accessControlService).requireAdmin(principal);
    when(repository.findTopByPartnerIdOrderByUpdatedAtDesc(1L)).thenReturn(Optional.empty());
    when(repository.save(any(ManualAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ManualAcceptance saved = manualAcceptanceService.save(form, principal);

    assertThat(saved.getSessionId()).isNull();
    verify(accessControlService, org.mockito.Mockito.times(2)).requireAdmin(principal);
  }

  @Test
  void save_withoutSessionShouldRejectMerchantQc() {
    PartnerConfig partner = partner(1L, "Merchant A");
    ManualAcceptanceForm form = form(1L, null);

    SitUserPrincipal principal = principal("qc@merchant.com", UserRole.MERCHANT_QC);
    when(partnerService.requireAccessible(1L, principal)).thenReturn(partner);
    doThrow(new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này"))
        .when(accessControlService)
        .requireAdmin(principal);

    assertThatThrownBy(() -> manualAcceptanceService.save(form, principal))
        .isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void findLatestBySessionShouldDelegateToRepository() {
    ManualAcceptance acceptance = new ManualAcceptance();
    when(repository.findTopBySessionIdOrderByUpdatedAtDesc(7L))
        .thenReturn(Optional.of(acceptance));

    assertThat(manualAcceptanceService.findLatestBySession(7L)).contains(acceptance);
  }

  private static PartnerConfig partner(Long id, String name) {
    PartnerConfig partner = new PartnerConfig();
    partner.setId(id);
    partner.setName(name);
    return partner;
  }

  private static TestSession session(Long id, Long partnerId) {
    TestSession session = new TestSession();
    session.setId(id);
    session.setPartnerId(partnerId);
    return session;
  }

  private static ManualAcceptanceForm form(Long partnerId, Long sessionId) {
    ManualAcceptanceForm form = new ManualAcceptanceForm();
    form.setPartnerId(partnerId);
    form.setSessionId(sessionId);
    return form;
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
