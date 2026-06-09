package com.enterprise.microservice.health;

import com.enterprise.microservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final ProductRepository productRepository;

    @Override
    public Health health() {
        try {
            long productCount = productRepository.count();

            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("productCount", productCount)
                    .withDetail("status", "Available")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Unavailable")
                    .build();
        }
    }
}