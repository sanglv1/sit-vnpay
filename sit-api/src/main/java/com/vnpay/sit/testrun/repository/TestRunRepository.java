package com.vnpay.sit.testrun.repository;

import com.vnpay.sit.testrun.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    List<TestRun> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Page<TestRun> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    Page<TestRun> findBySessionIdInOrderByCreatedAtDesc(Collection<Long> sessionIds, Pageable pageable);

    List<TestRun> findBySessionIdInOrderByCreatedAtDesc(Collection<Long> sessionIds);

    Page<TestRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TestRun> findByPartnerIdOrderByCreatedAtDesc(Long partnerId, Pageable pageable);

    long countByPassedTrue();

    long countByPassedFalse();

    long countByPassedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByPassedFalseAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByPassedTrueAndSessionIdInAndCreatedAtBetween(
            Collection<Long> sessionIds, LocalDateTime from, LocalDateTime to);

    long countByPassedFalseAndSessionIdInAndCreatedAtBetween(
            Collection<Long> sessionIds, LocalDateTime from, LocalDateTime to);

    long countByPassedTrueAndSessionIdIn(Collection<Long> sessionIds);

    long countByPassedFalseAndSessionIdIn(Collection<Long> sessionIds);

    Page<TestRun> findByPassedFalseAndSessionIdInOrderByCreatedAtDesc(
            Collection<Long> sessionIds, Pageable pageable);

    Page<TestRun> findByPassedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT tr.sessionId, COUNT(DISTINCT tr.testCase)
            FROM TestRun tr
            WHERE tr.sessionId IN :sessionIds
              AND tr.testCase IN :autoCases
              AND tr.passed = true
            GROUP BY tr.sessionId
            """)
    List<Object[]> countDistinctPassedAutoCasesBySessionIds(
            Collection<Long> sessionIds,
            Collection<com.vnpay.sit.model.TestCaseType> autoCases
    );

    @Query("""
            SELECT tr FROM TestRun tr
            WHERE tr.sessionId = :sessionId
              AND tr.id IN (
                SELECT MAX(tr2.id) FROM TestRun tr2
                WHERE tr2.sessionId = :sessionId
                GROUP BY tr2.testCase
              )
            ORDER BY tr.createdAt DESC
            """)
    List<TestRun> findLatestPerTestCaseBySessionId(Long sessionId);

    @Query("""
            SELECT tr.sessionId, COUNT(tr.id)
            FROM TestRun tr
            WHERE tr.sessionId IN :sessionIds
            GROUP BY tr.sessionId
            """)
    List<Object[]> countRunsBySessionIds(Collection<Long> sessionIds);
}
