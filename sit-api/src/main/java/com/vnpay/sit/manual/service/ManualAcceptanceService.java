package com.vnpay.sit.manual.service;

import com.vnpay.sit.manual.dto.ManualAcceptanceForm;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.repository.ManualAcceptanceRepository;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ManualAcceptanceService {

    private final ManualAcceptanceRepository repository;
    private final PartnerService partnerService;

    public ManualAcceptanceService(ManualAcceptanceRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
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
    public ManualAcceptance save(ManualAcceptanceForm form) {
        PartnerConfig partner = partnerService.findById(form.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

        ManualAcceptance entity;
        if (form.getId() != null) {
            entity = repository.findById(form.getId()).orElse(new ManualAcceptance());
        } else if (form.getSessionId() != null) {
            entity = repository.findTopBySessionIdOrderByUpdatedAtDesc(form.getSessionId())
                    .orElse(new ManualAcceptance());
        } else {
            entity = repository.findTopByPartnerIdOrderByUpdatedAtDesc(partner.getId())
                    .orElse(new ManualAcceptance());
        }

        entity.setPartnerId(partner.getId());
        entity.setSessionId(form.getSessionId());
        entity.setPartnerName(partner.getName());
        entity.setReturnSuccessTxnRef(trim(form.getReturnSuccessTxnRef()));
        entity.setReturnSuccessImage(form.getReturnSuccessImage());
        entity.setReturnFailedTxnRef(trim(form.getReturnFailedTxnRef()));
        entity.setReturnFailedImage(form.getReturnFailedImage());
        entity.setExceptionHandled(form.getExceptionHandled());
        entity.setWhitelistIpPassed(form.getWhitelistIpPassed());
        entity.setLogStoragePassed(form.getLogStoragePassed());
        entity.setNote(form.getNote());

        return repository.save(entity);
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
