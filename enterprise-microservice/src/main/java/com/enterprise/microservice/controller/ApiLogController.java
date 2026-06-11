package com.enterprise.microservice.controller;

import com.enterprise.microservice.entity.ApiLogEntity;
import com.enterprise.microservice.repository.ApiLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/api-logs")
@RequiredArgsConstructor
@Tag(name = "API Logs", description = "Admin — view persisted API request/response logs")
public class ApiLogController {

    private final ApiLogRepository apiLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all API logs (paginated)")
    public ResponseEntity<Page<ApiLogEntity>> getAllLogs(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(apiLogRepository.findAll(pageable));
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get logs by username")
    public ResponseEntity<Page<ApiLogEntity>> getLogsByUser(
            @PathVariable String username,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                apiLogRepository.findByUsernameOrderByCreatedAtDesc(username, pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get logs within a date-time range")
    public ResponseEntity<Page<ApiLogEntity>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(apiLogRepository.findByDateRange(from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get single log entry by ID")
    public ResponseEntity<ApiLogEntity> getLogById(@PathVariable Long id) {
        return apiLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}