package com.enterprise.microservice.aspect;

import com.enterprise.microservice.annotation.ApiLog;
import com.enterprise.microservice.entity.ApiLogEntity;
import com.enterprise.microservice.repository.ApiLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP Aspect — intercepts every @ApiLog-annotated controller method.
 *
 * Captures:
 *   - traceId from MDC (set by RequestTracingFilter)
 *   - authenticated username from SecurityContextHolder
 *   - HTTP method, endpoint, controller class, method name
 *   - serialized request args (with sensitive field masking)
 *   - serialized response body
 *   - HTTP status code
 *   - execution time in milliseconds
 *   - client IP from MDC
 *   - error message if an exception was thrown
 *
 * Persistence is ASYNCHRONOUS — the log write never adds latency
 * to the API response. It fires-and-forgets on a separate thread.
 *
 * Truncation: request/response bodies are capped at 5000 characters
 * to prevent MEDIUMTEXT exhaustion from unexpectedly large payloads.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private static final int MAX_BODY_LENGTH = 5000;

    private final ObjectMapper      objectMapper;
    private final ApiLogRepository  apiLogRepository;

    @Around("@annotation(apiLog)")
    public Object logApiCall(ProceedingJoinPoint joinPoint, ApiLog apiLog) throws Throwable {

        StopWatch sw = new StopWatch();
        sw.start();

        // Capture request context BEFORE method execution
        HttpServletRequest  httpRequest  = currentRequest();
        String traceId   = MDC.get("traceId");
        String username  = resolveUsername();
        String clientIp  = MDC.get("clientIp");
        String httpMethod  = httpRequest != null ? httpRequest.getMethod() : "UNKNOWN";
        String endpoint    = httpRequest != null ? httpRequest.getRequestURI() : "UNKNOWN";
        String className   = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName  = joinPoint.getSignature().getName();

        // Serialize request body (with field masking)
        String requestBody = null;
        if (apiLog.logRequestBody()) {
            requestBody = serializeArgs(joinPoint.getArgs(),
                    Set.of(apiLog.maskFields()), joinPoint);
        }

        String responseBody = null;
        Integer httpStatus  = null;
        String  errorMsg    = null;
        Object  result      = null;

        try {
            result = joinPoint.proceed();

            // Extract status code from ResponseEntity if present
            if (result instanceof ResponseEntity<?> re) {
                httpStatus = re.getStatusCode().value();
                if (apiLog.logResponseBody()) {
                    responseBody = serialize(re.getBody());
                }
            } else {
                // Resolve status from the current response
                HttpServletResponse resp = currentResponse();
                httpStatus = resp != null ? resp.getStatus() : 200;
                if (apiLog.logResponseBody()) {
                    responseBody = serialize(result);
                }
            }
            return result;

        } catch (Throwable ex) {
            httpStatus = 500;
            errorMsg   = ex.getClass().getSimpleName() + ": " + truncate(ex.getMessage(), 500);
            throw ex;

        } finally {
            sw.stop();
            long durationMs = sw.getTotalTimeMillis();

            // Build entity values as effectively final locals for the async lambda
            final String  finalTraceId      = traceId;
            final String  finalUsername     = username;
            final String  finalHttpMethod   = httpMethod;
            final String  finalEndpoint     = endpoint;
            final String  finalClassName    = className;
            final String  finalMethodName   = methodName;
            final String  finalRequestBody  = truncate(requestBody,  MAX_BODY_LENGTH);
            final String  finalResponseBody = truncate(responseBody, MAX_BODY_LENGTH);
            final Integer finalHttpStatus   = httpStatus;
            final String  finalClientIp     = clientIp;
            final String  finalErrorMsg     = errorMsg;

            // Async persistence — does NOT block the HTTP response thread
            persistAsync(finalTraceId, finalUsername, finalHttpMethod, finalEndpoint,
                    finalClassName, finalMethodName, finalRequestBody, finalResponseBody,
                    finalHttpStatus, durationMs, finalClientIp, finalErrorMsg);
        }
    }

    // -----------------------------------------------------------------------
    // Async DB write — runs on Spring's @Async thread pool, not request thread
    // -----------------------------------------------------------------------

    @Async("apiLogExecutor")
    public void persistAsync(String traceId, String username, String httpMethod,
                             String endpoint, String className, String methodName,
                             String requestBody, String responseBody, Integer httpStatus,
                             long executionMs, String clientIp, String errorMessage) {
        try {
            ApiLogEntity entity = new ApiLogEntity();
            entity.setTraceId(traceId);
            entity.setUsername(username);
            entity.setHttpMethod(httpMethod);
            entity.setEndpoint(endpoint);
            entity.setControllerClass(className);
            entity.setMethodName(methodName);
            entity.setRequestBody(requestBody);
            entity.setResponseBody(responseBody);
            entity.setHttpStatus(httpStatus);
            entity.setExecutionMs(executionMs);
            entity.setClientIp(clientIp);
            entity.setErrorMessage(errorMessage);
            apiLogRepository.save(entity);
        } catch (Exception ex) {
            // Never let a logging failure affect the main request
            log.error("Failed to persist API log asynchronously: {}", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletResponse currentResponse() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getResponse();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serializes method arguments, masking any fields whose names are in maskFields.
     * Non-serializable args fall back to their toString().
     */
    private String serializeArgs(Object[] args, Set<String> maskFields,
                                 ProceedingJoinPoint joinPoint) {
        if (args == null || args.length == 0) return "[]";
        try {
            MethodSignature sig    = (MethodSignature) joinPoint.getSignature();
            String[]        names  = sig.getParameterNames();
            ObjectNode      node   = objectMapper.createObjectNode();

            for (int i = 0; i < args.length; i++) {
                String paramName = (names != null && i < names.length) ? names[i] : "arg" + i;
                if (args[i] == null) {
                    node.putNull(paramName);
                } else if (maskFields.contains(paramName)) {
                    node.put(paramName, "***");
                } else {
                    try {
                        node.set(paramName, objectMapper.valueToTree(args[i]));
                    } catch (Exception e) {
                        node.put(paramName, args[i].toString());
                    }
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) + "...[truncated]" : value;
    }
}