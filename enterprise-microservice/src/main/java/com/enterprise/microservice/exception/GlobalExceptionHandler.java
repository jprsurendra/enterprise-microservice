package com.enterprise.microservice.exception;

import com.enterprise.microservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Centralized API error handler.
 *
 * Handler priority notes:
 * - AccessDeniedException: handles RBAC failures (authenticated user, wrong role).
 *   JWT auth failures (unauthenticated) are handled by JwtAuthenticationEntryPoint
 *   at the filter level — they never reach this handler.
 * - NoResourceFoundException: Spring MVC 6.x (Boot 3.x) replacement for
 *   NoHandlerFoundException — handles 404s for unknown paths.
 * - The generic Exception fallback catches anything unhandled, returning 500.
 *   It deliberately logs the full stack trace for observability.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied for [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.FORBIDDEN,
                ErrorCode.ERR_AUTH_004.getCode(),
                "You do not have permission to perform this action.",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return error.getObjectName() + ": " + error.getDefaultMessage();
                })
                .sorted()
                .collect(Collectors.joining("; "));

        log.warn("Validation failed on [{}]: {}", request.getRequestURI(), details);

        return build(HttpStatus.BAD_REQUEST,
                ErrorCode.ERR_DATA_VALIDATION.getCode(),
                "Validation failed — " + details,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body on [{}]: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.BAD_REQUEST,
                ErrorCode.ERR_DATA_VALIDATION.getCode(),
                "Request body is malformed or missing.",
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found: [{}] {}", request.getMethod(), request.getRequestURI());

        return build(HttpStatus.NOT_FOUND,
                ErrorCode.ERR_DATA_NOT_FOUND.getCode(),
                "The requested resource does not exist.",
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not allowed: [{}] {}", request.getMethod(), request.getRequestURI());

        return build(HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCode.ERR_SYS_001.getCode(),
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.",
                request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception on [{}]: [{}] {}",
                request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        return build(HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getErrorCode(),
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        // Full stack trace logged — critical for production debugging
        log.error("Unhandled exception on [{} {}]", request.getMethod(), request.getRequestURI(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.ERR_SYS_001.getCode(),
                "An unexpected error occurred. Please contact support.",
                request);
    }

    // -----------------------------------------------------------------------
    // Builder helper
    // -----------------------------------------------------------------------

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String errorCode,
                                                   String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                MDC.get("traceId"));
        return ResponseEntity.status(status).body(body);
    }
}