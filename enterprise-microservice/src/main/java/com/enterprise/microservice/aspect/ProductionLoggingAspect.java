package com.enterprise.microservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * PROD Aspect — zero-noise performance monitoring.
 *
 * Logging policy:
 * - Normal execution (< 500ms): SILENT. No log line emitted.
 * - Degraded execution (500ms–2000ms): WARN with method name and duration.
 * - Slow execution (> 2000ms): ERROR — SLA breach.
 * - Any exception: ERROR with class name only (no message — avoids PII leak).
 *
 * No input/output serialization. No PII in log lines.
 */
@Slf4j
@Aspect
@Component
@Profile("prod")
public class ProductionLoggingAspect {

    private static final long WARN_THRESHOLD_MS  = 500;
    private static final long ERROR_THRESHOLD_MS = 2000;

    @Around("@within(org.springframework.stereotype.Service)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            Object result = joinPoint.proceed();
            sw.stop();
            long ms = sw.getTotalTimeMillis();

            if (ms > ERROR_THRESHOLD_MS) {
                log.error("[PROD] SLA breach — {} took {}ms (threshold: {}ms)", method, ms, ERROR_THRESHOLD_MS);
            } else if (ms > WARN_THRESHOLD_MS) {
                log.warn("[PROD] Slow method — {} took {}ms", method, ms);
            }
            // Under threshold: intentionally silent — no log line
            return result;

        } catch (Throwable t) {
            sw.stop();
            // Log exception class only — never log message (may contain PII/sensitive data)
            log.error("[PROD] Method failed — {} threw {} after {}ms",
                    method, t.getClass().getName(), sw.getTotalTimeMillis());
            throw t;
        }
    }
}