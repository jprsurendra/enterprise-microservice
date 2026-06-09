package com.enterprise.microservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}