package com.vnpay.sit.session.repository;

import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.session.SessionCompletionFilter;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.testrun.entity.TestRun;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TestSessionSpecifications {

    private TestSessionSpecifications() {
    }

    public static Specification<TestSession> ownedBy(String ownerEmail) {
        if (!StringUtils.hasText(ownerEmail)) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("createdByEmail")),
                ownerEmail.trim().toLowerCase()
        );
    }

    public static Specification<TestSession> matchesSearch(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String term = q.trim().toLowerCase();
        String pattern = "%" + term + "%";
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("tmnCode"), "")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("pendingTxnRef"), "")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("confirmedTxnRef"), "")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("partnerName"), "")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("createdByEmail"), "")), pattern));

            Subquery<Integer> txnSub = query.subquery(Integer.class);
            Root<TestRun> runRoot = txnSub.from(TestRun.class);
            txnSub.select(cb.literal(1));
            txnSub.where(
                    cb.equal(runRoot.get("sessionId"), root.get("id")),
                    cb.like(cb.lower(cb.coalesce(runRoot.get("txnRef"), "")), pattern)
            );
            predicates.add(cb.exists(txnSub));

            if (term.matches("\\d+")) {
                predicates.add(cb.equal(root.get("id"), Long.parseLong(term)));
            }

            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<TestSession> matchesCompletion(
            SessionCompletionFilter filter,
            Collection<TestCaseType> autoCases
    ) {
        if (filter == null || filter == SessionCompletionFilter.ALL || autoCases.isEmpty()) {
            return null;
        }
        int autoTotal = autoCases.size();
        return (root, query, cb) -> {
            Subquery<Long> passedCount = query.subquery(Long.class);
            Root<TestRun> runRoot = passedCount.from(TestRun.class);
            passedCount.select(cb.countDistinct(runRoot.get("testCase")));
            passedCount.where(
                    cb.equal(runRoot.get("sessionId"), root.get("id")),
                    runRoot.get("testCase").in(autoCases),
                    cb.isTrue(runRoot.get("passed"))
            );

            return switch (filter) {
                case NOT_STARTED -> cb.equal(passedCount, 0L);
                case COMPLETED -> cb.equal(passedCount, (long) autoTotal);
                case IN_PROGRESS -> cb.and(
                        cb.greaterThan(passedCount, 0L),
                        cb.lessThan(passedCount, (long) autoTotal)
                );
                default -> cb.conjunction();
            };
        };
    }
}
