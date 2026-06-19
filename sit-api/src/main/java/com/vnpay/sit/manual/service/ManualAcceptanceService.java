package com.vnpay.sit.manual.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.manual.dto.ManualAcceptanceForm;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.repository.ManualAcceptanceRepository;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.entity.TestSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ManualAcceptanceService {

    private final ManualAcceptanceRepository repository;
    private final PartnerService partnerService;
    private final AccessControlService accessControlService;

    public ManualAcceptanceService(
            ManualAcceptanceRepository repository,
            PartnerService partnerService,
            AccessControlService accessControlService
    ) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.accessControlService = accessControlService;
    }

    public Optional<ManualAcceptance> findLatestBySession(Long sessionId) {
        return repository.findTopBySessionIdOrderByUpdatedAtDesc(sessionId);
    }

    public Optional<ManualAcceptance> findLatestByPartner(Long partnerId) {
        return repository.findTopByPartnerIdOrderByUpdatedAtDesc(partnerId);
    }

    public Optional<ManualAcceptance> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public ManualAcceptance save(ManualAcceptanceForm form, SitUserPrincipal principal) {
        PartnerConfig partner = partnerService.requireAccessible(form.getPartnerId(), principal);
        TestSession session = resolveSession(form, principal);

        ManualAcceptance entity = resolveEntity(form, session, principal);
        if (entity.getSessionId() != null) {
            accessControlService.requireSessionAccess(entity.getSessionId(), principal);
        } else {
            accessControlService.requireAdmin(principal);
        }

        entity.setPartnerId(partner.getId());
        entity.setSessionId(session != null ? session.getId() : null);
        entity.setPartnerName(partner.getName());
        entity.setReturnSuccessTxnRef(trim(form.getReturnSuccessTxnRef()));
        entity.setReturnSuccessImage(resolveImage(form.getReturnSuccessImage(), entity.getReturnSuccessImage()));
        entity.setReturnFailedTxnRef(trim(form.getReturnFailedTxnRef()));
        entity.setReturnFailedImage(resolveImage(form.getReturnFailedImage(), entity.getReturnFailedImage()));
        entity.setExceptionHandled(form.getExceptionHandled());
        entity.setWhitelistIpPassed(form.getWhitelistIpPassed());
        entity.setLogStoragePassed(form.getLogStoragePassed());
        entity.setNote(form.getNote());

        return repository.save(entity);
    }

    private TestSession resolveSession(ManualAcceptanceForm form, SitUserPrincipal principal) {
        if (form.getSessionId() == null) {
            return null;
        }
        TestSession session = accessControlService.requireSession(form.getSessionId());
        accessControlService.requireSessionAccess(session, principal);
        if (!session.getPartnerId().equals(form.getPartnerId())) {
            throw new IllegalArgumentException("Terminal không khớp với phiên kiểm thử");
        }
        return session;
    }

    private ManualAcceptance resolveEntity(
            ManualAcceptanceForm form,
            TestSession session,
            SitUserPrincipal principal
    ) {
        if (form.getId() != null) {
            ManualAcceptance existing = repository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi nghiệm thu"));
            if (existing.getSessionId() != null) {
                accessControlService.requireSessionAccess(existing.getSessionId(), principal);
            } else {
                accessControlService.requireAdmin(principal);
            }
            if (session != null && existing.getSessionId() != null
                    && !existing.getSessionId().equals(session.getId())) {
                throw new IllegalArgumentException("Bạn không có quyền cập nhật bản ghi này");
            }
            return existing;
        }
        if (session != null) {
            return repository.findTopBySessionIdOrderByUpdatedAtDesc(session.getId())
                    .orElse(new ManualAcceptance());
        }
        accessControlService.requireAdmin(principal);
        return repository.findTopByPartnerIdOrderByUpdatedAtDesc(form.getPartnerId())
                .orElse(new ManualAcceptance());
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }

    /** Giữ ảnh cũ khi client không gửi lại (tránh mất ảnh khi payload bị cắt). */
    private static String resolveImage(String incoming, String existing) {
        return StringUtils.hasText(incoming) ? incoming.trim() : existing;
    }
}
