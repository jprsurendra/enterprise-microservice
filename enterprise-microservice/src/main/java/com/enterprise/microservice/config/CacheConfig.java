package com.enterprise.microservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    // Default TTL — 5 minutes for anything not specifically configured
    private RedisCacheConfiguration defaultConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(

                // Permissions — short TTL, security-sensitive
                "permissions",        defaultConfig().entryTtl(Duration.ofSeconds(60)),

                // Roles — short TTL
                "roles",              defaultConfig().entryTtl(Duration.ofSeconds(60)),

                // Vendor profile — medium TTL
                "vendor_profile",     defaultConfig().entryTtl(Duration.ofMinutes(10)),

                // Lender config — long TTL, rarely changes
                "lender_config",      defaultConfig().entryTtl(Duration.ofHours(1)),

                // PO data from SHPP — short TTL, government data
                "shpp_po",            defaultConfig().entryTtl(Duration.ofMinutes(5)),

                // Reference data (states, districts, categories) — very long TTL
                "reference_data",     defaultConfig().entryTtl(Duration.ofHours(6))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig())
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}