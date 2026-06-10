package com.enterprise.microservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Custom database health indicator.
 *
 * Uses a lightweight 'SELECT 1' ping via JdbcTemplate — NOT productRepository.count().
 * count() performs a full table scan, acquires locks, and is semantically wrong
 * (it tests business data, not connectivity).
 *
 * Reports:
 *   UP   — DB reachable, includes response time
 *   DOWN — DB unreachable, includes sanitized error (no stack trace in health endpoint)
 */
@Slf4j
@Component("database")   // Appears as "database" in /actuator/health response
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long responseMs = System.currentTimeMillis() - start;

            return Health.up()
                    .withDetail("engine",      "MySQL")
                    .withDetail("pingMs",       responseMs)
                    .withDetail("checkedAt",    Instant.now().toString())
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());

            return Health.down()
                    .withDetail("engine",   "MySQL")
                    .withDetail("error",    e.getClass().getSimpleName() + ": " + e.getMessage())
                    .withDetail("checkedAt", Instant.now().toString())
                    .build();
        }
    }
}