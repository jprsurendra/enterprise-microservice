package com.enterprise.microservice.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ERR_AUTH_001("ERR_AUTH_001", "Invalid credentials provided"),
    ERR_AUTH_002("ERR_AUTH_002", "JWT token is expired"),
    ERR_AUTH_003("ERR_AUTH_003", "JWT token is invalid"),
    ERR_AUTH_004("ERR_AUTH_004", "Access denied"),
    ERR_DATA_NOT_FOUND("ERR_DATA_001", "Resource not found"),
    ERR_DATA_VALIDATION("ERR_DATA_002", "Validation failed"),
    ERR_SYS_001("ERR_SYS_001", "Internal server error");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}