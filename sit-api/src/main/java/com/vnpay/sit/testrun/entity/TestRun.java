package com.vnpay.sit.testrun.entity;

import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_run")
@Getter
@Setter
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentFlow flow;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_type", nullable = false, length = 10)
    private CallbackType callbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_case", nullable = false, length = 30)
    private TestCaseType testCase;

    @Column(name = "txn_ref", length = 100)
    private String txnRef;

    @Column(name = "target_url", length = 1000)
    private String targetUrl;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /** Full HTTP GET URL sent to merchant (IPN / Return URL). */
    @Column(name = "request_url", columnDefinition = "TEXT")
    private String requestUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "expected_rsp_code", length = 10)
    private String expectedRspCode;

    @Column(name = "actual_rsp_code", length = 10)
    private String actualRspCode;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
