package com.enterprise.microservice.security;

import com.enterprise.microservice.dto.ApiErrorResponse;
import com.enterprise.microservice.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
/**
 * Invoked when an unauthenticated request reaches a secured endpoint.
 * Returns a structured 401 JSON response — NOT a redirect to a login page.
 * Without this, Spring Security returns a 403 for missing/invalid JWT,
 * which is semantically wrong (403 = authenticated but forbidden).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to [{}]: {}",
                request.getRequestURI(), authException.getMessage());
//        String traceId = MDC.get("traceId")
        String traceId = MDC.get("traceId") != null
                ? MDC.get("traceId")
                : UUID.randomUUID().toString();   // fallback — MDC may be empty on error dispatch

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.ERR_AUTH_003.getCode(),
                "Authentication required. Provide a valid Bearer token.",
                request.getRequestURI(),
                traceId
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}