package com.enterprise.microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_logs", indexes = {
        @Index(name = "idx_intg_trace_id",   columnList = "trace_id"),
        @Index(name = "idx_intg_name",       columnList = "integration_name"),
        @Index(name = "idx_intg_created_at", columnList = "created_at"),
        @Index(name = "idx_intg_success",    columnList = "success")
})
@Getter
@Setter
@NoArgsConstructor
public class IntegrationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36)
    private String traceId;

    @Column(nullable = false, length = 100)
    private String integrationName;     // e.g. "PAYMENT_GATEWAY", "SMS_SERVICE"

    @Column(nullable = false, length = 100)
    private String operation;           // e.g. "CHARGE_CARD", "SEND_OTP"

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 1000)
    private String targetUrl;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestPayload;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String responsePayload;

    private Integer httpStatus;

    private Long executionMs;

    @Column(nullable = false)
    private Boolean success = false;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(length = 100)
    private String triggeredBy;          // username who triggered this call

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}