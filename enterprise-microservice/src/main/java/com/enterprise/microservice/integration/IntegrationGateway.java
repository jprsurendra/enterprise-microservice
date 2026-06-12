package com.enterprise.microservice.integration;
//package com.enterprise.microservice.service;
//package com.enterprise.microservice.integration;

import com.enterprise.microservice.entity.IntegrationLogEntity;
import com.enterprise.microservice.dto.IntegrationRequest;
import com.enterprise.microservice.dto.IntegrationResponse;
//import com.enterprise.microservice.integration.dto.IntegrationRequest;
//import com.enterprise.microservice.integration.dto.IntegrationResponse;
import com.enterprise.microservice.repository.IntegrationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
//import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * IntegrationGateway — Universal HTTP wrapper for all third-party API calls.
 *
 * Every outgoing HTTP call made through this gateway:
 *   1. Injects traceId into outgoing request headers (X-Trace-Id)
 *   2. Serializes request/response payloads
 *   3. Measures execution time
 *   4. Retries on transient failures (5xx, timeout)
 *   5. Persists a full log entry to integration_logs table (async)
 *   6. Returns a consistent IntegrationResponse — callers never deal with exceptions
 *
 * Usage:
 *   IntegrationRequest req = IntegrationRequest.builder()
 *       .integrationName("PAYMENT_GATEWAY")
 *       .operation("CHARGE_CARD")
 *       .httpMethod("POST")
 *       .url("https://api.payments.com/v1/charge")
 *       .body(chargeDto)
 *       .headers(Map.of("X-Api-Key", apiKey))
 *       .timeoutSeconds(15)
 *       .maxRetries(2)
 *       .build();
 *
 *   IntegrationResponse resp = integrationGateway.call(req);
 *   if (!resp.isSuccess()) { // handle failure }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationGateway {

    private final RestClient                restClient;
    private final ObjectMapper              objectMapper;
    private final IntegrationLogRepository  integrationLogRepository;

    /**
     * Executes an outbound HTTP call with retry logic and full DB logging.
     */
    public IntegrationResponse call(IntegrationRequest request) {
        String traceId     = MDC.get("traceId");
        String triggeredBy = resolveCurrentUser();
        int    retryCount  = 0;
        int    maxRetries  = request.getMaxRetries() > 0 ? request.getMaxRetries() : 3;

//        IntegrationResponse response = null;
        long totalStartMs = System.currentTimeMillis();
        IntegrationResponse response = null;

        while (retryCount <= maxRetries) {
            long attemptStart = System.currentTimeMillis();
            try {
                response = executeHttp(request, traceId);
                long attemptMs = System.currentTimeMillis() - attemptStart;

                if (!response.isSuccess() && response.getStatusCode() >= 500
                        && retryCount < maxRetries) {
                    log.warn("[{}] {} → {} (attempt {}/{}) — retrying...",
                            request.getIntegrationName(), request.getOperation(),
                            response.getStatusCode(), retryCount + 1, maxRetries + 1);
                    retryCount++;
                    Thread.sleep(calculateBackoffMs(retryCount));
                    continue;
                }
                break;

            } catch (ResourceAccessException e) {
                long attemptMs = System.currentTimeMillis() - attemptStart;
                log.warn("[{}] {} timeout on attempt {}/{}: {}",
                        request.getIntegrationName(), request.getOperation(),
                        retryCount + 1, maxRetries + 1, e.getMessage());

                if (retryCount < maxRetries) {
                    retryCount++;
                    try { Thread.sleep(calculateBackoffMs(retryCount)); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                long totalMs = System.currentTimeMillis() - totalStartMs;
                response = IntegrationResponse.failure(0, "Timeout: " + e.getMessage(),
                        totalMs, retryCount);
                break;

            } catch (Exception e) {
                long totalMs = System.currentTimeMillis() - totalStartMs;
                log.error("[{}] {} unexpected error: {}",
                        request.getIntegrationName(), request.getOperation(), e.getMessage());
                response = IntegrationResponse.failure(0, e.getMessage(), totalMs, retryCount);
                break;
            }
        }
        /*
        Fix #6 — Fix StopWatch reuse across retries in IntegrationGateway.call()
        Why it was wrong: Spring's StopWatch throws IllegalStateException if you call start() when it's already running, and calling start() after stop() on the same instance accumulates total time across all retry attempts rather than measuring per-attempt time.

        StopWatch sw = new StopWatch();

        while (retryCount <= maxRetries) {
            sw.start();
            try {
                response = executeHttp(request, traceId);
                sw.stop();

                // Retry on 5xx server errors
                if (!response.isSuccess() && response.getStatusCode() >= 500
                        && retryCount < maxRetries) {
                    log.warn("[{}] {} → {} (attempt {}/{}) — retrying...",
                            request.getIntegrationName(), request.getOperation(),
                            response.getStatusCode(), retryCount + 1, maxRetries + 1);
                    retryCount++;
                    Thread.sleep(calculateBackoffMs(retryCount));
                    continue;
                }

                break;  // success or non-retryable failure

            } catch (ResourceAccessException e) {
                // Network timeout — retryable
                if (sw.isRunning()) sw.stop();
                log.warn("[{}] {} timeout on attempt {}/{}: {}",
                        request.getIntegrationName(), request.getOperation(),
                        retryCount + 1, maxRetries + 1, e.getMessage());

                if (retryCount < maxRetries) {
                    retryCount++;
                    try { Thread.sleep(calculateBackoffMs(retryCount)); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                response = IntegrationResponse.failure(0, "Timeout: " + e.getMessage(),
                        sw.getTotalTimeMillis(), retryCount);
                break;

            } catch (Exception e) {
                if (sw.isRunning()) sw.stop();
                log.error("[{}] {} unexpected error: {}",
                        request.getIntegrationName(), request.getOperation(), e.getMessage());
                response = IntegrationResponse.failure(0, e.getMessage(),
                        sw.getTotalTimeMillis(), retryCount);
                break;
            }
        }
        */
        final int finalRetries = retryCount;
        final IntegrationResponse finalResponse = response;
        persistLogAsync(request, finalResponse, traceId, triggeredBy, finalRetries);

        return response;
    }

    // -----------------------------------------------------------------------
    // HTTP Execution
    // -----------------------------------------------------------------------
    private IntegrationResponse executeHttp(IntegrationRequest request, String traceId) {
        long startMs = System.currentTimeMillis();

        try {
            String serializedBody = request.getBody() != null
                    ? objectMapper.writeValueAsString(request.getBody())
                    : null;

            RestClient.RequestBodySpec spec = restClient
                    .method(HttpMethod.valueOf(request.getHttpMethod().toUpperCase()))
                    .uri(request.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", traceId != null ? traceId : "");

            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    spec.header(entry.getKey(), entry.getValue());
                }
            }

            if (serializedBody != null) {
                spec.body(serializedBody);
            }

            // toEntity() captures the actual HTTP status code (201, 204, etc.)
            // body(String.class) would return only the body and lose the status
            ResponseEntity<String> entity = spec.retrieve().toEntity(String.class);
            long durationMs = System.currentTimeMillis() - startMs;

            return IntegrationResponse.success(
                    entity.getStatusCode().value(),
                    entity.getBody(),
                    durationMs,
                    0);

        } catch (HttpClientErrorException e) {
            // 4xx — not retryable
            return IntegrationResponse.failure(e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    System.currentTimeMillis() - startMs, 0);

        } catch (HttpServerErrorException e) {
            // 5xx — retryable (caller decides)
            return IntegrationResponse.failure(e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    System.currentTimeMillis() - startMs, 0);

        } catch (Exception e) {
            throw new RuntimeException("Integration HTTP call failed: " + e.getMessage(), e);
        }
    }

//  Fix #7 — executeHttp() status code on 2xx success
//Why it was wrong: RestClient.retrieve().body(String.class) returns only the body — the actual HTTP status (could be 201, 204, etc.) is lost and hardcoded as 200.
//
//    private IntegrationResponse executeHttp(IntegrationRequest request, String traceId) {
//        StopWatch sw = new StopWatch();
//        sw.start();
//
//        try {
//            String serializedBody = request.getBody() != null
//                    ? objectMapper.writeValueAsString(request.getBody())
//                    : null;
//
//            RestClient.RequestBodySpec spec = restClient
//                    .method(HttpMethod.valueOf(request.getHttpMethod().toUpperCase()))
//                    .uri(request.getUrl())
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .header("X-Trace-Id", traceId != null ? traceId : "");
//
//            // Apply caller-supplied headers (auth tokens, API keys, etc.)
//            if (request.getHeaders() != null) {
//                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
//                    spec.header(entry.getKey(), entry.getValue());
//                }
//            }
//
//            if (serializedBody != null) {
//                spec.body(serializedBody);
//            }
//
//            /*String responseBody = spec.retrieve().body(String.class);
//            sw.stop();
//
//            return IntegrationResponse.success(200, responseBody, sw.getTotalTimeMillis(), 0);*/
//            // AFTER — use toEntity() to capture actual status:
//            ResponseEntity<String> entity = spec.retrieve().toEntity(String.class);
//            long durationMs = System.currentTimeMillis() - startMs;
//            return IntegrationResponse.success(
//                    entity.getStatusCode().value(),
//                    entity.getBody(),
//                    durationMs,
//                    0
//            );
//
//        } catch (HttpClientErrorException e) {
//            if (sw.isRunning()) sw.stop();
//            // 4xx — not retryable
//            return IntegrationResponse.failure(e.getStatusCode().value(),
//                    e.getResponseBodyAsString(), sw.getTotalTimeMillis(), 0);
//
//        } catch (HttpServerErrorException e) {
//            if (sw.isRunning()) sw.stop();
//            // 5xx — retryable (caller decides)
//            return IntegrationResponse.failure(e.getStatusCode().value(),
//                    e.getResponseBodyAsString(), sw.getTotalTimeMillis(), 0);
//
//        } catch (Exception e) {
//            if (sw.isRunning()) sw.stop();
//            // Wrap into RuntimeException so the retry loop in call() can catch it
//            // without requiring executeHttp() to declare throws Exception
//            throw new RuntimeException("Integration HTTP call failed: " + e.getMessage(), e);
//        }
//    }

    // -----------------------------------------------------------------------
    // Async DB Logging
    // -----------------------------------------------------------------------

    @Async("integrationExecutor")
    public void persistLogAsync(IntegrationRequest request, IntegrationResponse response,
                                String traceId, String triggeredBy, int retryCount) {
        try {
            IntegrationLogEntity log = new IntegrationLogEntity();
            log.setTraceId(traceId);
            log.setIntegrationName(request.getIntegrationName());
            log.setOperation(request.getOperation());
            log.setHttpMethod(request.getHttpMethod());
            log.setTargetUrl(request.getUrl());
            log.setTriggeredBy(triggeredBy);
            log.setRetryCount(retryCount);

            if (request.isLogRequestBody() && request.getBody() != null) {
                try {
                    log.setRequestPayload(truncate(objectMapper.writeValueAsString(request.getBody()), 5000));
                } catch (Exception e) {
                    log.setRequestPayload(request.getBody().toString());
                }
            }

            if (response != null) {
                log.setSuccess(response.isSuccess());
                log.setHttpStatus(response.getStatusCode());
                log.setExecutionMs(response.getExecutionMs());
                log.setErrorMessage(truncate(response.getErrorMessage(), 1000));
                if (request.isLogResponseBody()) {
                    log.setResponsePayload(truncate(response.getBody(), 5000));
                }
            }

            integrationLogRepository.save(log);
        } catch (Exception ex) {
//            Slf4j.class.getSimpleName(); // no-op; just log
//            this.log.error("Failed to persist integration log: {}", ex.getMessage());
            log.error("Failed to persist integration log: {}", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Exponential backoff: 1s, 2s, 4s ... capped at 10s */
    private long calculateBackoffMs(int attempt) {
        return Math.min(1000L * (long) Math.pow(2, attempt - 1), 10_000L);
    }

    private String resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) + "...[truncated]" : value;
    }
}




/* =====================================================================================================================
    Example — Using the Gateway in a Service
    // How to call a third-party payment API from your service:


import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IntegrationGateway gateway;

    public PaymentResult chargeCard(ChargeRequest charge) {
        IntegrationRequest request = IntegrationRequest.builder()
                .integrationName("PAYMENT_GATEWAY")
                .operation("CHARGE_CARD")
                .httpMethod("POST")
                .url("https://api.payments.example.com/v1/charges")
                .body(charge)
                .headers(Map.of("X-Api-Key", paymentApiKey))
                .timeoutSeconds(15)
                .maxRetries(2)
                .logRequestBody(true)
                .logResponseBody(true)
                .build();

        IntegrationResponse response = gateway.call(request);

        if (!response.isSuccess()) {
            throw new BusinessException(ErrorCode.ERR_SYS_001,
                    "Payment processing failed: " + response.getErrorMessage());
        }

        return objectMapper.readValue(response.getBody(), PaymentResult.class);
    }
}



   ===================================================================================================================== */
