package com.enterprise.microservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pool for async API log persistence.
 *
 * Isolated from the default async executor so that a log-write backlog
 * never starves other async tasks in the application.
 *
 * Sizing rationale:
 *   corePoolSize  = 2  — always-on threads for steady-state log throughput
 *   maxPoolSize   = 10 — burst capacity under high traffic
 *   queueCapacity = 500 — buffer for sudden request spikes
 *   keepAlive     = 60s — idle threads above core are released after 60s
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "apiLogExecutor")
    public Executor apiLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("api-log-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }

    @Bean(name = "integrationExecutor")
    public Executor integrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("integration-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}