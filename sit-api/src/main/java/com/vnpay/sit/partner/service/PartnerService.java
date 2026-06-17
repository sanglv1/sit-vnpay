package com.vnpay.sit.partner.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.partner.dto.PartnerForm;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerService {

    private final PartnerConfigRepository repository;
    private final AccessControlService accessControlService;

    public PartnerService(PartnerConfigRepository repository, AccessControlService accessControlService) {
        this.repository = repository;
        this.accessControlService = accessControlService;
    }

    public List<PartnerConfig> findAllForPrincipal(SitUserPrincipal principal) {
        if (accessControlService.isAdmin(principal)) {
            return repository.findAll();
        }
        return findCreatedByPrincipal(principal);
    }

    public List<PartnerConfig> findAllActiveForPrincipal(SitUserPrincipal principal) {
        if (accessControlService.isAdmin(principal)) {
            return repository.findByActiveTrueOrderByNameAsc();
        }
        String email = accessControlService.currentUserEmail(principal);
        if (!StringUtils.hasText(email)) {
            return List.of();
        }
        return repository.findByActiveTrueAndCreatedByEmailIgnoreCaseOrderByNameAsc(email.trim());
    }

    public PartnerConfig requireAccessible(Long id, SitUserPrincipal principal) {
        PartnerConfig partner = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));
        accessControlService.requirePartnerAccess(partner, principal);
        return partner;
    }

    public Optional<PartnerConfig> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public PartnerConfig save(PartnerForm form, SitUserPrincipal principal) {
        PartnerConfig entity;
        if (form.getId() != null) {
            entity = requireAccessible(form.getId(), principal);
        } else {
            entity = new PartnerConfig();
            String creator = accessControlService.currentUserEmail(principal);
            if (!StringUtils.hasText(creator)) {
                throw new IllegalArgumentException("Không xác định được người tạo Terminal");
            }
            entity.setCreatedByEmail(creator.trim().toLowerCase());
        }
        entity.setName(form.getName().trim());
        entity.setFlow(form.getFlow());
        entity.setTmnCode(form.getTmnCode().trim());
        entity.setSecretKey(form.getSecretKey().trim());
        entity.setReturnUrl(trimToEmpty(form.getReturnUrl()));
        entity.setIpnUrl(form.getIpnUrl().trim());
        entity.setNote(form.getNote());
        entity.setActive(form.isActive());
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id, SitUserPrincipal principal) {
        requireAccessible(id, principal);
        repository.deleteById(id);
    }

    public PartnerForm toForm(PartnerConfig entity) {
        PartnerForm form = new PartnerForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setFlow(entity.getFlow());
        form.setTmnCode(entity.getTmnCode());
        form.setSecretKey(entity.getSecretKey());
        form.setReturnUrl(entity.getReturnUrl());
        form.setIpnUrl(entity.getIpnUrl());
        form.setNote(entity.getNote());
        form.setActive(entity.isActive());
        return form;
    }

    private List<PartnerConfig> findCreatedByPrincipal(SitUserPrincipal principal) {
        String email = accessControlService.currentUserEmail(principal);
        if (!StringUtils.hasText(email)) {
            return List.of();
        }
        return repository.findByCreatedByEmailIgnoreCaseOrderByNameAsc(email.trim());
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
