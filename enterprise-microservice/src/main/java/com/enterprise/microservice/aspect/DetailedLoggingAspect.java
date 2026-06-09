package com.enterprise.microservice.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
@Profile("dev")
public class DetailedLoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@within(org.springframework.stereotype.Service) || @within(org.springframework.web.bind.annotation.RestController)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        StopWatch stopWatch = new StopWatch();

        // Log input parameters
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                String argsJson = objectMapper.writeValueAsString(args);
                log.info("DEV Mode - Entering: {}.{} with args: {}", className, methodName, argsJson);
            } catch (Exception e) {
                log.info("DEV Mode - Entering: {}.{} with {} arguments", className, methodName, args.length);
            }
        } else {
            log.info("DEV Mode - Entering: {}.{} with no arguments", className, methodName);
        }

        stopWatch.start();
        Object result;
        try {
            result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            try {
                String resultJson = objectMapper.writeValueAsString(result);
                log.info("DEV Mode - Exiting: {}.{} with result: {} (execution time: {} ms)",
                        className, methodName, resultJson, executionTime);
            } catch (Exception e) {
                log.info("DEV Mode - Exiting: {}.{} (execution time: {} ms)", className, methodName, executionTime);
            }
        } catch (Exception e) {
            stopWatch.stop();
            log.error("DEV Mode - Error in {}.{} after {} ms: {}", className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage());
            throw e;
        }

        return result;
    }
}