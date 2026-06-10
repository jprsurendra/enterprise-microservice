package com.enterprise.microservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal diagnostic controller.
 * DB credentials and env values are NEVER exposed via any endpoint —
 * the original /test-env endpoint was a security anti-pattern.
 */
@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/admin/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminInfo() {
        return ResponseEntity.ok("Admin endpoint operational.");
    }
}