package com.vnpay.sit.session.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_session")
@Getter
@Setter
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Column(name = "tmn_code", length = 20)
    private String tmnCode;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

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
