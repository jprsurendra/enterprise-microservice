package com.enterprise.microservice.filter;

import com.enterprise.microservice.entity.ApiLogEntity;
import com.enterprise.microservice.repository.ApiLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RequestTracingFilter — runs on EVERY request, logs ALL API calls to api_logs table.
 *
 * Responsibilities:
 * 1. Assign a unique traceId (UUID) to every request — injected into MDC so it
 *    appears in ALL log lines for the full request lifecycle.
 * 2. Extract the true client IP (X-Forwarded-For → X-Real-IP → remoteAddr).
 * 3. Echo traceId back as X-Trace-Id response header for client-side correlation.
 * 4. Wrap request/response in caching wrappers so body can be read without
 *    consuming the stream (servlet streams are read-once by default).
 * 5. Persist a full ApiLogEntity record asynchronously after every request —
 *    zero impact on response latency.
 * 6. Automatically skip body capture for sensitive endpoints (login)
 *    and binary/multipart content.
 *
 * Why filter-level instead of @ApiLog annotation?
 *   - Annotations must be added manually to every method — easy to forget.
 *   - Filter runs on 100% of requests automatically, including 401/403/404 errors.
 *   - Error responses (Spring's own 401/404) are also captured — @ApiLog AOP
 *     only runs on methods that execute successfully past security filters.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RequestTracingFilter extends OncePerRequestFilter {

    private final ApiLogRepository apiLogRepository;

    private static final String MDC_TRACE_ID    = "traceId";
    private static final String MDC_CLIENT_IP   = "clientIp";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    // Max body size captured in logs — prevents huge payloads filling MEDIUMTEXT
    private static final int MAX_BODY_LENGTH = 5000;

    // Endpoints where request body must NOT be logged (contains credentials)
    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/api/auth/login"
    );

    // Endpoints where response body must NOT be logged (contains JWT token)
    private static final Set<String> SKIP_RESPONSE_PATHS = Set.of(
            "/api/auth/login"
    );

    // Skip DB logging for these paths entirely — too noisy, no business value
    private static final Set<String> SKIP_LOG_PATHS = Set.of(
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/api/v1/ping",
            "/favicon.ico"
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]{2,39}$"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Accept incoming traceId from upstream systems (iFMS, SHPP etc.)
        // If none provided, generate our own
        String incomingTraceId = request.getHeader("X-Trace-Id");
        String traceId = (incomingTraceId != null && !incomingTraceId.isBlank())
                ? incomingTraceId
                : UUID.randomUUID().toString();

        String clientIp = resolveClientIp(request);

        MDC.put(MDC_TRACE_ID,  traceId);
        MDC.put(MDC_CLIENT_IP, clientIp);
        response.setHeader(HEADER_TRACE_ID, traceId);

        // Wrap request and response so their bodies can be read multiple times
        // Without this, reading the body in the filter consumes it —
        // the controller would receive an empty body
        ContentCachingRequestWrapper  wrappedRequest  = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        String errorMessage = null;

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception ex) {
            errorMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int  status     = wrappedResponse.getStatus();

            // Always log the one-line summary to application log
            log.info("REQUEST method={} uri={} status={} duration={}ms ip={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    durationMs,
                    clientIp);

            // Persist to DB asynchronously — only if path is not in skip list
            String uri = request.getRequestURI();
            if (!SKIP_LOG_PATHS.contains(uri)) {
                String requestBody  = captureRequestBody(wrappedRequest, uri);
                String responseBody = captureResponseBody(wrappedResponse, uri);

                persistAsync(
                        traceId,
                        resolveUsername(),
                        request.getMethod(),
                        uri,
                        requestBody,
                        responseBody,
                        status,
                        durationMs,
                        clientIp,
                        errorMessage
                );
            }

            // CRITICAL: copy response body back to the actual response stream.
            // ContentCachingResponseWrapper buffers the body internally —
            // without this call, the client receives an empty response body.
            wrappedResponse.copyBodyToResponse();

            // CRITICAL: clear MDC — threads are reused in the thread pool.
            // Without this, the next request on this thread inherits the old traceId.
            MDC.clear();
        }
    }

    // -----------------------------------------------------------------------
    // Async DB Persistence
    // -----------------------------------------------------------------------

    @Async("apiLogExecutor")
    public void persistAsync(String traceId, String username, String httpMethod,
                             String endpoint, String requestBody, String responseBody,
                             int httpStatus, long executionMs,
                             String clientIp, String errorMessage) {
        try {
            ApiLogEntity entity = new ApiLogEntity();
            entity.setTraceId(traceId);
            entity.setUsername(username);
            entity.setHttpMethod(httpMethod);
            entity.setEndpoint(endpoint);
            entity.setRequestBody(requestBody);
            entity.setResponseBody(responseBody);
            entity.setHttpStatus(httpStatus);
            entity.setExecutionMs(executionMs);
            entity.setClientIp(clientIp);
            entity.setErrorMessage(errorMessage);
            // controllerClass and methodName not available at filter level —
            // left null (populated only when @ApiLog AOP is also used)
            apiLogRepository.save(entity);
        } catch (Exception ex) {
            // Never let logging failure affect the main request
            log.error("Failed to persist request log to api_logs: {}", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Body Capture Helpers
    // -----------------------------------------------------------------------

    private String captureRequestBody(ContentCachingRequestWrapper request, String uri) {
        // Never log body for sensitive paths (login — contains password)
        if (SENSITIVE_PATHS.contains(uri)) {
            return "[MASKED — sensitive endpoint]";
        }

        // Skip binary and multipart content
        String contentType = request.getContentType();
        if (contentType != null && (
                contentType.contains("multipart/") ||
                        contentType.contains("application/octet-stream"))) {
            return "[BINARY — not logged]";
        }

        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) return null;

        String raw = new String(body, StandardCharsets.UTF_8);
        return truncate(raw, MAX_BODY_LENGTH);
    }

    private String captureResponseBody(ContentCachingResponseWrapper response, String uri) {
        // Never log response for paths that return JWT tokens
        if (SKIP_RESPONSE_PATHS.contains(uri)) {
            return "[MASKED — contains token]";
        }

        String contentType = response.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            return null;   // Only capture JSON responses
        }

        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) return null;

        String raw = new String(body, StandardCharsets.UTF_8);
        return truncate(raw, MAX_BODY_LENGTH);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the authenticated username from the request attribute.
     * JwtAuthenticationFilter stores the username in request attributes
     * after successful token validation — we read it here after the chain executes.
     */
    private String resolveUsername() {
        // Read from MDC — JwtAuthenticationFilter should set this
        // Falls back to "anonymous" if not authenticated
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String candidate = xForwardedFor.split(",")[0].strip();
            if (isValidIp(candidate)) return candidate;
            log.warn("X-Forwarded-For invalid value — falling back. Value: [{}]",
                    xForwardedFor.length() > 50
                            ? xForwardedFor.substring(0, 50) + "..." : xForwardedFor);
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && isValidIp(xRealIp.strip())) return xRealIp.strip();

        String remoteAddr = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }

    private boolean isValidIp(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen
                ? value.substring(0, maxLen) + "...[truncated]"
                : value;
    }
}