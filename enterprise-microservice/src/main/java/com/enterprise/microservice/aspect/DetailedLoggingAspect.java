package com.enterprise.microservice.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * DEV/UAT Aspect — verbose logging with full input/output serialization.
 * Only active under 'dev' or 'uat' profiles.
 *
 * Pointcut targets @Service beans only — NOT @RestController.
 * Controllers are already covered by RequestTracingFilter; intercepting
 * them here would produce duplicate log entries per request.
 */
@Slf4j
@Aspect
@Component
@Profile({"dev", "uat"})
@RequiredArgsConstructor
public class DetailedLoggingAspect {

    // Injected from Spring context — shared, thread-safe singleton
    private final ObjectMapper objectMapper;

    @Around("@within(org.springframework.stereotype.Service)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className  = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                log.info("[DEV] >>> {}.{}() args={}", className, methodName,
                        objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                log.info("[DEV] >>> {}.{}() args=[{} arg(s) — not serializable]",
                        className, methodName, args.length);
            }
        } else {
            log.info("[DEV] >>> {}.{}() args=[]", className, methodName);
        }

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            Object result = joinPoint.proceed();
            sw.stop();
            try {
                log.info("[DEV] <<< {}.{}() result={} duration={}ms",
                        className, methodName,
                        objectMapper.writeValueAsString(result),
                        sw.getTotalTimeMillis());
            } catch (Exception e) {
                log.info("[DEV] <<< {}.{}() duration={}ms [result not serializable]",
                        className, methodName, sw.getTotalTimeMillis());
            }
            return result;
        } catch (Throwable t) {
            sw.stop();
            log.error("[DEV] !!! {}.{}() threw {} after {}ms — message: {}",
                    className, methodName, t.getClass().getSimpleName(),
                    sw.getTotalTimeMillis(), t.getMessage());
            throw t;
        }
    }
}