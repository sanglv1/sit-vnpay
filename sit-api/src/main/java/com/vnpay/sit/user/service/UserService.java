package com.vnpay.sit.user.service;

import com.vnpay.sit.config.SitBootstrapProperties;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.user.dto.ResetPasswordForm;
import com.vnpay.sit.user.dto.UserForm;
import com.vnpay.sit.user.entity.SitUser;
import com.vnpay.sit.user.repository.SitUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final SitUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SitBootstrapProperties bootstrapProperties;

    public UserService(
            SitUserRepository repository,
            PasswordEncoder passwordEncoder,
            SitBootstrapProperties bootstrapProperties
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    public List<SitUser> search(String q) {
        return repository.search(q != null ? q.trim() : null);
    }

    public Optional<SitUser> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public SitUser save(UserForm form) {
        boolean isCreate = form.getId() == null;
        SitUser entity = isCreate
                ? new SitUser()
                : repository.findById(form.getId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String email = form.getEmail().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, entity.getId() != null ? entity.getId() : -1L)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        if (isCreate && !StringUtils.hasText(form.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        entity.setEmail(email);
        entity.setFullName(form.getFullName().trim());
        entity.setRole(form.getRole());
        entity.setActive(form.isActive());

        if (StringUtils.hasText(form.getPassword())) {
            if (form.getPassword().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
            }
            entity.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }

        return repository.save(entity);
    }

    @Transactional
    public SitUser resetPassword(Long id, ResetPasswordForm form) {
        SitUser entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        entity.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        return repository.save(entity);
    }

    @Transactional
    public SitUser updateStatus(Long id, boolean active) {
        SitUser entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        entity.setActive(active);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy người dùng");
        }
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    @Transactional
    public void seedDefaultAdminIfEmpty() {
        if (repository.count() > 0) {
            return;
        }

        String email = bootstrapProperties.getAdminEmail() != null
                ? bootstrapProperties.getAdminEmail().trim().toLowerCase()
                : "";
        String password = bootstrapProperties.getAdminPassword();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn(
                    "Chưa cấu hình SIT_ADMIN_EMAIL / SIT_ADMIN_PASSWORD — bỏ qua tạo tài khoản admin mặc định"
            );
            return;
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("SIT_ADMIN_PASSWORD phải có ít nhất 6 ký tự");
        }

        SitUser admin = new SitUser();
        admin.setFullName(
                StringUtils.hasText(bootstrapProperties.getAdminFullName())
                        ? bootstrapProperties.getAdminFullName().trim()
                        : "System Admin"
        );
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        repository.save(admin);
        log.info("Đã tạo tài khoản admin mặc định: {}", email);
    }
}
