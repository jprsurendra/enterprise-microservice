package com.enterprise.microservice.dto;
//package com.enterprise.microservice.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class IntegrationRequest {
    private String              integrationName;  // "PAYMENT_GATEWAY"
    private String              operation;        // "CHARGE_CARD"
    private String              httpMethod;       // "POST"
    private String              url;
    private Object              body;             // will be serialized to JSON
    private Map<String, String> headers;          // extra headers (auth, content-type)
    private int                 timeoutSeconds;   // default 10
    private int                 maxRetries;       // default 3
    private boolean             logRequestBody;   // default true
    private boolean             logResponseBody;  // default true
}