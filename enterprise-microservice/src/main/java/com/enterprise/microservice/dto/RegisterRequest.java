package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "New user registration request")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, and underscores")
    @Schema(description = "Unique login username", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 100)
    @Schema(description = "Unique email address", example = "john.doe@enterprise.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#])[A-Za-z\\d@$!%*?&_#]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @Schema(description = "Password — min 8 chars, must include upper, lower, digit, special char",
            example = "Secret@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "Optional display name", example = "John Doe")
    private String fullName;
}