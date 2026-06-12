package com.enterprise.microservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Permission name must be uppercase letters, digits, and underscores only")
    private String name;

    @NotBlank(message = "Resource is required")
    @Size(max = 100)
    private String resource;

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(READ|CREATE|UPDATE|DELETE|MANAGE)$", message = "Action must be one of: READ, CREATE, UPDATE, DELETE, MANAGE")
    private String action;

    @Size(max = 255)
    private String description;
}