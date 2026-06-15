package com.vnpay.sit.partner.service;

import com.vnpay.sit.partner.dto.PartnerForm;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerService {

    private final PartnerConfigRepository repository;

    public PartnerService(PartnerConfigRepository repository) {
        this.repository = repository;
    }

    public List<PartnerConfig> findAllActive() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    public List<PartnerConfig> findAll() {
        return repository.findAll();
    }

    public Optional<PartnerConfig> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public PartnerConfig save(PartnerForm form) {
        PartnerConfig entity = form.getId() != null
                ? repository.findById(form.getId()).orElse(new PartnerConfig())
                : new PartnerConfig();
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
    public void delete(Long id) {
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

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
