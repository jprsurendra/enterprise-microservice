package com.enterprise.microservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String CLIENT_IP_KEY = "clientIp";


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            String traceId = UUID.randomUUID().toString();
            String clientIp = extractClientIp(request);

            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(CLIENT_IP_KEY, clientIp);

            long startTime = System.currentTimeMillis();

            chain.doFilter(requestWrapper, responseWrapper);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Request processed - Method: {}, URI: {}, Status: {}, Duration: {} ms, Client IP: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration, clientIp);

        } finally {
            MDC.clear();
            responseWrapper.copyBodyToResponse();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String clientIp = null;

        // Check X-Forwarded-For header
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            clientIp = ips[0].trim();
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "127.0.0.1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }

        return clientIp;
    }
}