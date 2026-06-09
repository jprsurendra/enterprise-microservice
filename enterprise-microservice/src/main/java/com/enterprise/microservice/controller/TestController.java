package com.enterprise.microservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${DB_USERNAME:not_set}")
    private String dbUsername;

    @GetMapping("/test-env")
    public String testEnv() {
        return "DB Username: " + dbUsername;
    }

    @GetMapping("/health-check")
    public String healthCheck() {
        return "Application is running!";
    }
}