package com.vnpay.sit.manual.repository;

import com.vnpay.sit.manual.entity.ManualAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManualAcceptanceRepository extends JpaRepository<ManualAcceptance, Long> {

    Optional<ManualAcceptance> findTopByPartnerIdOrderByUpdatedAtDesc(Long partnerId);

    Optional<ManualAcceptance> findTopBySessionIdOrderByUpdatedAtDesc(Long sessionId);
}
