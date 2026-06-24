package com.vnpay.sit.manual.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "manual_acceptance")
@Getter
@Setter
public class ManualAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Column(name = "return_success_txn_ref", length = 100)
    private String returnSuccessTxnRef;

    @Column(name = "return_success_image", columnDefinition = "TEXT")
    private String returnSuccessImage;

    @Column(name = "return_failed_txn_ref", length = 100)
    private String returnFailedTxnRef;

    @Column(name = "return_failed_image", columnDefinition = "TEXT")
    private String returnFailedImage;

    /** Đã xử lý exception IPN (case ex → RspCode 99) */
    @Column(name = "exception_handled")
    private Boolean exceptionHandled;

    /** Whitelist IP: true = Đạt */
    @Column(name = "whitelist_ip_passed")
    private Boolean whitelistIpPassed;

    /** Lưu log: true = Đạt */
    @Column(name = "log_storage_passed")
    private Boolean logStoragePassed;

    @Column(length = 1000)
    private String note;

    /** JSON map: scenario key → requestLog / responseLog / image (biên bản Token mục 1–8). */
    @Column(name = "token_scenario_evidence", columnDefinition = "TEXT")
    private String tokenScenarioEvidence;

    /** JSON map: scenario key → requestLog / responseLog / image (biên bản Recurring mục 1–14). */
    @Column(name = "recurring_scenario_evidence", columnDefinition = "TEXT")
    private String recurringScenarioEvidence;

    /** JSON map: scenario key → requestLog / responseLog / image (biên bản Instalment mục 1–8). */
    @Column(name = "instalment_scenario_evidence", columnDefinition = "TEXT")
    private String instalmentScenarioEvidence;

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
