package com.enterprise.microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs", indexes = {
        @Index(name = "idx_api_logs_trace_id",   columnList = "trace_id"),
        @Index(name = "idx_api_logs_username",   columnList = "username"),
        @Index(name = "idx_api_logs_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ApiLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36)
    private String traceId;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String endpoint;

    @Column(length = 255)
    private String controllerClass;

    @Column(length = 255)
    private String methodName;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestBody;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String responseBody;

    private Integer httpStatus;

    private Long executionMs;

    @Column(length = 50)
    private String clientIp;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}