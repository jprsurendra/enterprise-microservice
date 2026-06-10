package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "JWT token response returned on successful authentication")
public class JwtResponse {

    @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Token type — always 'Bearer'", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token validity in seconds", example = "3600")
    private Long expiresIn;
}