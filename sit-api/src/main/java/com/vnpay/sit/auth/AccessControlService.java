package com.vnpay.sit.auth;

import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.repository.TestSessionRepository;
import com.vnpay.sit.testrun.entity.TestRun;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final TestSessionRepository sessionRepository;

    public AccessControlService(TestSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
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
        String ownerEmail = session.getCreatedByEmail();
        String currentEmail = principal != null ? principal.getUsername() : null;
        if (ownerEmail == null || currentEmail == null
                || !ownerEmail.equalsIgnoreCase(currentEmail.trim())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập phiên kiểm thử này");
        }
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
