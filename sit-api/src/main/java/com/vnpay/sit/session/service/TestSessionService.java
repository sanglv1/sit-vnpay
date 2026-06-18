package com.vnpay.sit.session.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.api.dto.TestSessionResponse;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.SessionCompletionFilter;
import com.vnpay.sit.session.dto.CreateSessionForm;
import com.vnpay.sit.session.dto.SaveSessionTestInputForm;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.session.repository.TestSessionSpecifications;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TestSessionService {

    private static final Set<TestCaseType> AUTO_CASES = EnumSet.copyOf(TestCaseType.ipnSuiteExecutionOrder());

    private final TestSessionRepository sessionRepository;
    private final TestRunRepository testRunRepository;
    private final PartnerService partnerService;
    private final AccessControlService accessControlService;

    public TestSessionService(
            TestSessionRepository sessionRepository,
            TestRunRepository testRunRepository,
            PartnerService partnerService,
            AccessControlService accessControlService
    ) {
        this.sessionRepository = sessionRepository;
        this.testRunRepository = testRunRepository;
        this.partnerService = partnerService;
        this.accessControlService = accessControlService;
    }

    public Page<TestSessionResponse> findAll(
            Pageable pageable,
            String q,
            SessionCompletionFilter completion,
            SitUserPrincipal principal
    ) {
        Specification<TestSession> spec;
        if (accessControlService.isAdmin(principal)) {
            spec = Specification.where(null);
        } else {
            spec = TestSessionSpecifications.ownedBy(accessControlService.currentUserEmail(principal));
        }
        Specification<TestSession> bySearch = TestSessionSpecifications.matchesSearch(q);
        if (bySearch != null) {
            spec = spec.and(bySearch);
        }
        Specification<TestSession> byCompletion = TestSessionSpecifications.matchesCompletion(completion, AUTO_CASES);
        if (byCompletion != null) {
            spec = spec.and(byCompletion);
        }

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<TestSession> page = sessionRepository.findAll(spec, sorted);
        return mapToResponses(page);
    }

    public TestSessionResponse getById(Long id, SitUserPrincipal principal) {
        TestSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));
        accessControlService.requireSessionAccess(session, principal);
        return toResponse(session, countPassedAutoCases(List.of(session.getId())));
    }

    public Optional<TestSession> findEntityById(Long id) {
        return sessionRepository.findById(id);
    }

    @Transactional
    public TestSessionResponse create(CreateSessionForm form, String createdByEmail, SitUserPrincipal principal) {
        PartnerConfig partner = partnerService.requireAccessible(form.getPartnerId(), principal);

        TestSession session = new TestSession();
        session.setPartnerId(partner.getId());
        session.setPartnerName(partner.getName());
        session.setTmnCode(partner.getTmnCode());
        session.setNote(form.getNote());
        session.setStatus("OPEN");
        if (StringUtils.hasText(createdByEmail)) {
            session.setCreatedByEmail(createdByEmail.trim().toLowerCase());
        }

        TestSession saved = sessionRepository.save(session);
        return toResponse(saved, java.util.Map.of(saved.getId(), 0));
    }

    @Transactional
    public void saveTestInput(
            Long id,
            SaveSessionTestInputForm form,
            SitUserPrincipal principal
    ) {
        TestSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));
        accessControlService.requireSessionAccess(session, principal);

        if (form.getPendingTxnRef() != null) {
            session.setPendingTxnRef(form.getPendingTxnRef().trim());
        }
        if (form.getPendingAmountVnd() != null) {
            session.setPendingAmountVnd(form.getPendingAmountVnd());
        }
        if (form.getConfirmedTxnRef() != null) {
            session.setConfirmedTxnRef(form.getConfirmedTxnRef().trim());
        }
        if (form.getConfirmedAmountVnd() != null) {
            session.setConfirmedAmountVnd(form.getConfirmedAmountVnd());
        }
        if (form.getFailedTxnRef() != null) {
            session.setFailedTxnRef(form.getFailedTxnRef().trim());
        }
        if (form.getFailedAmountVnd() != null) {
            session.setFailedAmountVnd(form.getFailedAmountVnd());
        }
        if (form.getWrongAmountVnd() != null) {
            session.setWrongAmountVnd(form.getWrongAmountVnd());
        }

        sessionRepository.save(session);
    }

    @Transactional
    public void mergeTestInputFromRun(
            Long sessionId,
            TestCaseType testCase,
            String txnRef,
            long amountVnd,
            Long wrongAmountVnd,
            SitUserPrincipal principal
    ) {
        if (sessionId == null || !StringUtils.hasText(txnRef)) {
            return;
        }
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));
        accessControlService.requireSessionAccess(session, principal);

        String normalizedTxnRef = txnRef.trim();
        if (testCase == TestCaseType.FAILED) {
            session.setFailedTxnRef(normalizedTxnRef);
            session.setFailedAmountVnd(amountVnd);
        } else if (testCase == TestCaseType.SUCCESS || testCase == TestCaseType.WRONG_AMOUNT) {
            session.setPendingTxnRef(normalizedTxnRef);
            session.setPendingAmountVnd(amountVnd);
        }
        if (testCase == TestCaseType.SUCCESS) {
            session.setConfirmedTxnRef(normalizedTxnRef);
            session.setConfirmedAmountVnd(amountVnd);
        }
        if (wrongAmountVnd != null) {
            session.setWrongAmountVnd(wrongAmountVnd);
        }
        sessionRepository.save(session);
    }

    private Page<TestSessionResponse> mapToResponses(Page<TestSession> page) {
        List<Long> sessionIds = page.getContent().stream().map(TestSession::getId).toList();
        java.util.Map<Long, Integer> passedBySession = countPassedAutoCases(sessionIds);
        return page.map(session -> toResponse(session, passedBySession));
    }

    private TestSessionResponse toResponse(TestSession session, java.util.Map<Long, Integer> passedBySession) {
        int autoPassed = passedBySession.getOrDefault(session.getId(), 0);
        return TestSessionResponse.from(session, autoPassed, AUTO_CASES.size());
    }

    private java.util.Map<Long, Integer> countPassedAutoCases(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, Integer> result = new java.util.HashMap<>();
        for (Object[] row : testRunRepository.countDistinctPassedAutoCasesBySessionIds(sessionIds, AUTO_CASES)) {
            Long sessionId = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            result.put(sessionId, count);
        }
        return result;
    }
}
