package com.enterprise.microservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place on any @RestController method to persist full request/response
 * details to the api_logs table via ApiLogAspect.
 *
 * Usage:
 *   @GetMapping("/{id}")
 *   @ApiLog(description = "Fetch product by ID")
 *   public ResponseEntity<ProductDto> getProductById(...) { ... }
 *
 * logRequestBody  — set false for endpoints that receive large binary uploads
 * logResponseBody — set false for endpoints that return large payloads
 * maskFields      — field names whose values will be replaced with "***" in logs
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {
    String description() default "";
    boolean logRequestBody  default true;
    boolean logResponseBody default true;
    String[] maskFields     default {};   // e.g. {"password", "creditCard"}
}