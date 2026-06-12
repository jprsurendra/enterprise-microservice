package com.enterprise.microservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication & Authorization
    ERR_AUTH_001("ERR_AUTH_001", "Invalid credentials"),
    ERR_AUTH_002("ERR_AUTH_002", "JWT token expired"),
    ERR_AUTH_003("ERR_AUTH_003", "JWT token invalid"),
    ERR_AUTH_004("ERR_AUTH_004", "Access denied"),
    ERR_DATA_001("ERR_DATA_001", "Resource not found"),
    ERR_DATA_002("ERR_DATA_002", "Validation failed"),
    ERR_DATA_003("ERR_DATA_003", "Resource already exists"),

    // Data: These aliases make service code self-documenting:
    ERR_DATA_NOT_FOUND("ERR_DATA_001", "Resource not found"),   // ← ADD if missing
    ERR_DATA_VALIDATION("ERR_DATA_002", "Input validation failed"),
    ERR_DATA_CONFLICT("ERR_DATA_003", "Resource already exists"), // ← confirm exists

    // System
    ERR_SYS_001("ERR_SYS_001", "Internal server error");


//    // Data
//    ERR_DATA_NOT_FOUND("ERR_DATA_001", "The requested resource was not found"),
//    ERR_DATA_VALIDATION("ERR_DATA_002", "Input validation failed"),
//    ERR_DATA_CONFLICT("ERR_DATA_003", "Resource already exists"),
//
//    // System
//    ERR_SYS_001("ERR_SYS_001", "An unexpected internal error occurred");

    private final String code;
    private final String defaultMessage;
    // ... constructor, getters
}