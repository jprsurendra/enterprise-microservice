package com.enterprise.microservice.dto;
//package com.enterprise.microservice.integration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntegrationResponse {
    private boolean success;
    private int     statusCode;
    private String  body;           // raw response body string
    private String  errorMessage;
    private long    executionMs;
    private int     retryCount;

    public static IntegrationResponse success(int status, String body, long ms, int retries) {
        return IntegrationResponse.builder()
                .success(true).statusCode(status).body(body)
                .executionMs(ms).retryCount(retries).build();
    }

    public static IntegrationResponse failure(int status, String error, long ms, int retries) {
        return IntegrationResponse.builder()
                .success(false).statusCode(status).errorMessage(error)
                .executionMs(ms).retryCount(retries).build();
    }
}
