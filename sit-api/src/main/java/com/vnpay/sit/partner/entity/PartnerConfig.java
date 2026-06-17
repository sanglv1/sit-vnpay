package com.vnpay.sit.partner.entity;

import com.vnpay.sit.model.PaymentFlow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_config")
@Getter
@Setter
public class PartnerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentFlow flow;

    @Column(name = "tmn_code", nullable = false, length = 20)
    private String tmnCode;

    @Column(name = "secret_key", nullable = false, length = 500)
    private String secretKey;

    @Column(name = "return_url", nullable = false, length = 500)
    private String returnUrl = "";

    @Column(name = "ipn_url", nullable = false, length = 500)
    private String ipnUrl;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private boolean active = true;

    /** Email người tạo Terminal — MERCHANT_QC chỉ xem/sửa Terminal do mình tạo. */
    @Column(name = "created_by_email", length = 200)
    private String createdByEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
