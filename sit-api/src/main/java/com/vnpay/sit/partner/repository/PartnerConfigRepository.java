package com.vnpay.sit.partner.repository;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.partner.entity.PartnerConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerConfigRepository extends JpaRepository<PartnerConfig, Long> {

    List<PartnerConfig> findByActiveTrueOrderByNameAsc();

    List<PartnerConfig> findByActiveTrueAndCreatedByEmailIgnoreCaseOrderByNameAsc(String createdByEmail);

    List<PartnerConfig> findByCreatedByEmailIgnoreCaseOrderByNameAsc(String createdByEmail);

    List<PartnerConfig> findByFlowAndActiveTrueOrderByNameAsc(PaymentFlow flow);

    long countByActiveTrueAndCreatedByEmailIgnoreCase(String createdByEmail);

    long countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(
            String createdByEmail,
            java.time.LocalDateTime createdAt
    );

    long countByActiveTrueAndCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String createdByEmail,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );

    long countByActiveTrueAndCreatedAtGreaterThanEqual(java.time.LocalDateTime createdAt);

    long countByActiveTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );
}
