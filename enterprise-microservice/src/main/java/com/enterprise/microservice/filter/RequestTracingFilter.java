package com.enterprise.microservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * High-priority request tracing filter.
 *
 * Responsibilities:
 * 1. Assign a unique traceId (UUID) to every request — injected into MDC so it
 *    appears in ALL log lines for the full request lifecycle, across all layers.
 * 2. Extract the true client IP by parsing X-Forwarded-For with IP validation
 *    to prevent log injection via spoofed headers.
 * 3. Echo the traceId back to the caller as 'X-Trace-Id' response header —
 *    enables client-side log correlation.
 * 4. Log a single summary line per request: method, URI, status, duration, IP.
 *
 * @Order(1) ensures this is the FIRST filter in the chain — traceId must be
 * in MDC before any other filter (including JwtAuthenticationFilter) logs.
 */
@Slf4j
@Component
@Order(1)
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String MDC_TRACE_ID   = "traceId";
    private static final String MDC_CLIENT_IP  = "clientIp";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    // Basic IPv4/IPv6 validation — prevents newline injection and garbage in logs
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]{2,39}$"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId  = UUID.randomUUID().toString();
        String clientIp = resolveClientIp(request);

        MDC.put(MDC_TRACE_ID,  traceId);
        MDC.put(MDC_CLIENT_IP, clientIp);

        // Echo traceId on response — allows client-side correlation with server logs
        response.setHeader(HEADER_TRACE_ID, traceId);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            log.info("REQUEST method={} uri={} status={} duration={}ms ip={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    clientIp);
            MDC.clear();
        }
    }

    /**
     * Resolves the originating client IP.
     *
     * X-Forwarded-For format: "client, proxy1, proxy2"
     * The leftmost IP is the true client. We take [0] and validate it.
     * If the header is absent, spoofed with invalid content, or the request
     * is direct, we fall back to getRemoteAddr().
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String candidate = xForwardedFor.split(",")[0].strip();
            if (isValidIp(candidate)) {
                return candidate;
            }
            log.warn("X-Forwarded-For contained invalid IP value — falling back to remoteAddr. Value: [{}]",
                    xForwardedFor.length() > 50 ? xForwardedFor.substring(0, 50) + "..." : xForwardedFor);
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && isValidIp(xRealIp.strip())) {
            return xRealIp.strip();
        }

        String remoteAddr = request.getRemoteAddr();
        // Normalize IPv6 loopback to a consistent representation
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }

    private boolean isValidIp(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }
}