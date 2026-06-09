package com.enterprise.microservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
@Profile("prod")
public class ProductionLoggingAspect {

    @Around("@within(org.springframework.stereotype.Service) || @within(org.springframework.web.bind.annotation.RestController)")
    public Object logPerformanceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        Object result;
        try {
            result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            if (executionTime > 1000) {
                log.warn("PROD - Slow method: {} took {} ms", methodName, executionTime);
            } else if (executionTime > 100) {
                log.debug("PROD - Method: {} took {} ms", methodName, executionTime);
            }

            log.info("PROD - Method executed: {}", methodName);
        } catch (Exception e) {
            stopWatch.stop();
            log.error("PROD - Method failed: {} after {} ms - Error: {}",
                    methodName, stopWatch.getTotalTimeMillis(), e.getClass().getSimpleName());
            throw e;
        }

        return result;
    }
}