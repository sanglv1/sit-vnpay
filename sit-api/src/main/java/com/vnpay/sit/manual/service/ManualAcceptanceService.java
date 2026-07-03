package com.vnpay.sit.manual.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.manual.InstalmentManualEvidenceSupport;
import com.vnpay.sit.manual.InstalmentManualScenario;
import com.vnpay.sit.manual.RecurringManualEvidenceSupport;
import com.vnpay.sit.manual.RecurringManualScenario;
import com.vnpay.sit.manual.TokenManualEvidenceSupport;
import com.vnpay.sit.manual.TokenManualScenario;
import com.vnpay.sit.manual.PreAuthManualEvidenceSupport;
import com.vnpay.sit.manual.PreAuthManualScenario;
import com.vnpay.sit.manual.QrDirectManualEvidenceSupport;
import com.vnpay.sit.manual.QrDirectManualScenario;
import com.vnpay.sit.manual.dto.ManualAcceptanceForm;
import com.vnpay.sit.manual.dto.TokenScenarioEvidence;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.repository.ManualAcceptanceRepository;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.entity.TestSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class ManualAcceptanceService {
    private static final int MANUAL_IMAGE_MAX_BYTES = 3 * 1024 * 1024;
    private static final String MANUAL_IMAGE_MAX_MESSAGE = "Ảnh tối đa 3MB";

    private final ManualAcceptanceRepository repository;
    private final PartnerService partnerService;
    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    public ManualAcceptanceService(
            ManualAcceptanceRepository repository,
            PartnerService partnerService,
            AccessControlService accessControlService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.accessControlService = accessControlService;
        this.objectMapper = objectMapper;
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
        applyTokenScenarioEvidence(entity, form);
        applyRecurringScenarioEvidence(entity, form);
        applyInstalmentScenarioEvidence(entity, form);
        applyQrDirectScenarioEvidence(entity, form);
        applyPreAuthScenarioEvidence(entity, form);
        validateImageSize(entity.getReturnSuccessImage());
        validateImageSize(entity.getReturnFailedImage());

        return repository.save(entity);
    }

    private void applyQrDirectScenarioEvidence(ManualAcceptance entity, ManualAcceptanceForm form) {
        Map<QrDirectManualScenario, TokenScenarioEvidence> existing =
                QrDirectManualEvidenceSupport.parse(entity.getQrDirectScenarioEvidence(), objectMapper);
        Map<QrDirectManualScenario, TokenScenarioEvidence> incoming =
                QrDirectManualEvidenceSupport.fromFormMap(form.getQrDirectScenarioEvidence());
        Map<QrDirectManualScenario, TokenScenarioEvidence> merged =
                QrDirectManualEvidenceSupport.mergeEvidence(existing, incoming, entity);
        validateScenarioEvidenceImages(merged);
        entity.setQrDirectScenarioEvidence(QrDirectManualEvidenceSupport.serialize(merged, objectMapper));
        QrDirectManualEvidenceSupport.syncLegacyReturnFields(entity, merged);
    }

    private void applyPreAuthScenarioEvidence(ManualAcceptance entity, ManualAcceptanceForm form) {
        Map<PreAuthManualScenario, TokenScenarioEvidence> existing =
                PreAuthManualEvidenceSupport.parse(entity.getPreauthScenarioEvidence(), objectMapper);
        Map<PreAuthManualScenario, TokenScenarioEvidence> incoming =
                PreAuthManualEvidenceSupport.fromFormMap(form.getPreauthScenarioEvidence());
        Map<PreAuthManualScenario, TokenScenarioEvidence> merged =
                PreAuthManualEvidenceSupport.mergeEvidence(existing, incoming, entity);
        validateScenarioEvidenceImages(merged);
        entity.setPreauthScenarioEvidence(PreAuthManualEvidenceSupport.serialize(merged, objectMapper));
        PreAuthManualEvidenceSupport.syncLegacyReturnFields(entity, merged);
    }

    private void applyInstalmentScenarioEvidence(ManualAcceptance entity, ManualAcceptanceForm form) {
        Map<InstalmentManualScenario, TokenScenarioEvidence> existing =
                InstalmentManualEvidenceSupport.parse(entity.getInstalmentScenarioEvidence(), objectMapper);
        Map<InstalmentManualScenario, TokenScenarioEvidence> incoming =
                InstalmentManualEvidenceSupport.fromFormMap(form.getInstalmentScenarioEvidence());
        Map<InstalmentManualScenario, TokenScenarioEvidence> merged =
                InstalmentManualEvidenceSupport.mergeEvidence(existing, incoming, entity);
        validateScenarioEvidenceImages(merged);
        entity.setInstalmentScenarioEvidence(InstalmentManualEvidenceSupport.serialize(merged, objectMapper));
        InstalmentManualEvidenceSupport.syncLegacyReturnFields(entity, merged);
    }

    private void applyRecurringScenarioEvidence(ManualAcceptance entity, ManualAcceptanceForm form) {
        Map<RecurringManualScenario, TokenScenarioEvidence> existing =
                RecurringManualEvidenceSupport.parse(entity.getRecurringScenarioEvidence(), objectMapper);
        Map<RecurringManualScenario, TokenScenarioEvidence> incoming =
                RecurringManualEvidenceSupport.fromFormMap(form.getRecurringScenarioEvidence());
        Map<RecurringManualScenario, TokenScenarioEvidence> merged =
                RecurringManualEvidenceSupport.mergeEvidence(existing, incoming, entity);
        validateScenarioEvidenceImages(merged);
        entity.setRecurringScenarioEvidence(RecurringManualEvidenceSupport.serialize(merged, objectMapper));
        RecurringManualEvidenceSupport.syncLegacyReturnFields(entity, merged);
    }

    private void applyTokenScenarioEvidence(ManualAcceptance entity, ManualAcceptanceForm form) {
        Map<TokenManualScenario, TokenScenarioEvidence> existing =
                TokenManualEvidenceSupport.parse(entity.getTokenScenarioEvidence(), objectMapper);
        Map<TokenManualScenario, TokenScenarioEvidence> incoming =
                TokenManualEvidenceSupport.fromFormMap(form.getTokenScenarioEvidence());
        Map<TokenManualScenario, TokenScenarioEvidence> merged =
                TokenManualEvidenceSupport.mergeEvidence(existing, incoming, entity);
        validateScenarioEvidenceImages(merged);
        entity.setTokenScenarioEvidence(TokenManualEvidenceSupport.serialize(merged, objectMapper));
        TokenManualEvidenceSupport.syncLegacyReturnFields(entity, merged);
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

    private static void validateScenarioEvidenceImages(Map<?, TokenScenarioEvidence> evidenceByScenario) {
        for (TokenScenarioEvidence evidence : evidenceByScenario.values()) {
            if (evidence == null) {
                continue;
            }
            validateImageSize(evidence.getImage());
        }
    }

    private static void validateImageSize(String imageDataUrl) {
        if (!StringUtils.hasText(imageDataUrl)) {
            return;
        }
        String value = imageDataUrl.trim();
        int commaIndex = value.indexOf(',');
        if (commaIndex <= 0 || commaIndex == value.length() - 1) {
            return;
        }
        String metadata = value.substring(0, commaIndex);
        String payload = value.substring(commaIndex + 1).trim();
        if (!metadata.contains("base64")) {
            return;
        }
        try {
            int decodedLength = Base64.getDecoder().decode(payload).length;
            if (decodedLength > MANUAL_IMAGE_MAX_BYTES) {
                throw new IllegalArgumentException(MANUAL_IMAGE_MAX_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            if (MANUAL_IMAGE_MAX_MESSAGE.equals(ex.getMessage())) {
                throw ex;
            }
            throw new IllegalArgumentException("Ảnh không hợp lệ");
        }
    }
}
