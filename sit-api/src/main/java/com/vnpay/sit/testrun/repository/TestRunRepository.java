package com.vnpay.sit.testrun.repository;

import com.vnpay.sit.testrun.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    List<TestRun> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Page<TestRun> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    Page<TestRun> findBySessionIdInOrderByCreatedAtDesc(Collection<Long> sessionIds, Pageable pageable);

    Page<TestRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TestRun> findByPartnerIdOrderByCreatedAtDesc(Long partnerId, Pageable pageable);

    long countByPassedTrue();

    long countByPassedFalse();

    long countByPassedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByPassedFalseAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    Page<TestRun> findByPassedFalseOrderByCreatedAtDesc(Pageable pageable);
}
