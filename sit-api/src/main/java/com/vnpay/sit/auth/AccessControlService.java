package com.vnpay.sit.auth;

import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.repository.PartnerConfigRepository;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccessControlService {

    private final TestSessionRepository sessionRepository;
    private final PartnerConfigRepository partnerRepository;

    public AccessControlService(
            TestSessionRepository sessionRepository,
            PartnerConfigRepository partnerRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.partnerRepository = partnerRepository;
    }

    public boolean isAdmin(SitUserPrincipal principal) {
        return principal != null && principal.getRole() == UserRole.ADMIN;
    }

    public void requireAdmin(SitUserPrincipal principal) {
        if (!isAdmin(principal)) {
            throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    public TestSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));
    }

    public void requireSessionAccess(Long sessionId, SitUserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        requireSessionAccess(requireSession(sessionId), principal);
    }

    public void requireSessionAccess(TestSession session, SitUserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        if (canAccessSession(session, principal)) {
            return;
        }
        throw new IllegalArgumentException("Bạn không có quyền truy cập phiên kiểm thử này");
    }

    /** MERCHANT_QC: phiên do mình tạo, hoặc phiên thuộc Terminal do mình sở hữu. */
    private boolean canAccessSession(TestSession session, SitUserPrincipal principal) {
        String currentEmail = normalizeEmail(currentUserEmail(principal));
        if (!StringUtils.hasText(currentEmail)) {
            return false;
        }
        String ownerEmail = normalizeEmail(session.getCreatedByEmail());
        if (StringUtils.hasText(ownerEmail) && ownerEmail.equals(currentEmail)) {
            return true;
        }
        if (session.getPartnerId() == null) {
            return false;
        }
        return partnerRepository.findById(session.getPartnerId())
                .map(PartnerConfig::getCreatedByEmail)
                .map(this::normalizeEmail)
                .filter(email -> email.equals(currentEmail))
                .isPresent();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public void requirePartnerAccess(PartnerConfig partner, SitUserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        String createdByEmail = normalizeEmail(partner.getCreatedByEmail());
        String currentEmail = normalizeEmail(currentUserEmail(principal));
        if (!StringUtils.hasText(createdByEmail) || !StringUtils.hasText(currentEmail)
                || !createdByEmail.equals(currentEmail)) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập Terminal này");
        }
    }

    public boolean canViewPartnerSecret(PartnerConfig partner, SitUserPrincipal principal) {
        if (isAdmin(principal)) {
            return true;
        }
        String createdByEmail = normalizeEmail(partner.getCreatedByEmail());
        String currentEmail = normalizeEmail(currentUserEmail(principal));
        return StringUtils.hasText(createdByEmail) && StringUtils.hasText(currentEmail)
                && createdByEmail.equals(currentEmail);
    }

    public void requireTestRunAccess(TestRun run, SitUserPrincipal principal) {
        if (isAdmin(principal) || run.getSessionId() == null) {
            if (!isAdmin(principal) && run.getSessionId() == null) {
                throw new IllegalArgumentException("Bạn không có quyền truy cập kết quả kiểm thử này");
            }
            return;
        }
        requireSessionAccess(run.getSessionId(), principal);
    }

    public String currentUserEmail(SitUserPrincipal principal) {
        return principal != null ? principal.getUsername() : null;
    }
}
