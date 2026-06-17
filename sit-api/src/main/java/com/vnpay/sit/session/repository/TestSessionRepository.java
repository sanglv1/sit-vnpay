package com.vnpay.sit.session.repository;

import com.vnpay.sit.session.entity.TestSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TestSessionRepository extends JpaRepository<TestSession, Long>, JpaSpecificationExecutor<TestSession> {

    Page<TestSession> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TestSession> findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(String createdByEmail, Pageable pageable);

    List<TestSession> findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(String createdByEmail);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqual(String createdByEmail, LocalDateTime createdAt);

    long countByCreatedByEmailIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String createdByEmail, LocalDateTime from, LocalDateTime to);
}
