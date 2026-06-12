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
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be 2-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Role name may only contain letters, digits, and underscores")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}