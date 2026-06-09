package com.enterprise.microservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private int statusCode;
    private String errorCode;
    private String message;
    private String path;
    private String traceId;

    public static ApiErrorResponse of(int statusCode, String errorCode, String message, String path, String traceId) {
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