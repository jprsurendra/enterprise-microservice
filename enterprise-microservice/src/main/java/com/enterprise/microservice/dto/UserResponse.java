package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Schema(description = "User profile data returned in responses — never contains password")
public class UserResponse {

    @Schema(description = "User database ID", example = "7")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john.doe@enterprise.com")
    private String email;

    @Schema(description = "Display name", example = "John Doe")
    private String fullName;

    @Schema(description = "Assigned roles", example = "[\"ROLE_USER\"]")
    private Set<String> roles;

    @Schema(description = "Account status", example = "true")
    private Boolean active;

    @Schema(description = "Registration timestamp")
    private LocalDateTime createdAt;
}