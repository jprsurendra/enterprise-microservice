package com.enterprise.microservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardized API error envelope returned on all non-2xx responses")
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Timestamp of the error", example = "2025-06-10 14:32:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int statusCode;

    @Schema(description = "Application-level error code", example = "ERR_DATA_001")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Product not found with id: 99")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/api/v1/products/99")
    private String path;

    @Schema(description = "Trace ID for log correlation — echo X-Trace-Id response header", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    public static ApiErrorResponse of(int statusCode, String errorCode,
                                      String message, String path, String traceId) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .statusCode(statusCode)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
    }
}