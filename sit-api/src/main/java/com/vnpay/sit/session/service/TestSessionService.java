package com.vnpay.sit.session.service;

import com.vnpay.sit.auth.AccessControlService;
import com.vnpay.sit.auth.SitUserPrincipal;
import com.vnpay.sit.api.dto.PageResponse;
import com.vnpay.sit.api.dto.TestSessionResponse;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.dto.CreateSessionForm;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Page<TestSessionResponse> findAll(Pageable pageable, SitUserPrincipal principal) {
        Page<TestSession> sessions = accessControlService.isAdmin(principal)
                ? sessionRepository.findAllByOrderByCreatedAtDesc(pageable)
                : sessionRepository.findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
                        accessControlService.currentUserEmail(principal),
                        pageable
                );
        return sessions.map(this::toResponse);
    }

    public TestSessionResponse getById(Long id, SitUserPrincipal principal) {
        TestSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));
        accessControlService.requireSessionAccess(session, principal);
        return toResponse(session);
    }

    public Optional<TestSession> findEntityById(Long id) {
        return sessionRepository.findById(id);
    }

    @Transactional
    public TestSessionResponse create(CreateSessionForm form, String createdByEmail) {
        PartnerConfig partner = partnerService.findById(form.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

        TestSession session = new TestSession();
        session.setPartnerId(partner.getId());
        session.setPartnerName(partner.getName());
        session.setTmnCode(partner.getTmnCode());
        session.setNote(form.getNote());
        session.setStatus("OPEN");
        session.setCreatedByEmail(createdByEmail);

        return toResponse(sessionRepository.save(session));
    }

    private TestSessionResponse toResponse(TestSession session) {
        List<TestRun> runs = testRunRepository.findBySessionIdOrderByCreatedAtDesc(session.getId());
        int autoTotal = AUTO_CASES.size();
        long autoPassed = runs.stream()
                .filter(r -> AUTO_CASES.contains(r.getTestCase()))
                .filter(TestRun::isPassed)
                .map(TestRun::getTestCase)
                .distinct()
                .count();
        return TestSessionResponse.from(session, (int) autoPassed, autoTotal);
    }
}
