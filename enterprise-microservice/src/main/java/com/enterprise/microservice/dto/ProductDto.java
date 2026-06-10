package com.enterprise.microservice.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Product API requests and responses.
 * Decouples the REST API contract from the JPA entity — critical for:
 * - Preventing schema leaks (createdAt, updatedAt, active never exposed in create/update requests)
 * - Independent API versioning
 * - Targeted validation rules per operation
 */
@Data
@Builder
public class ProductDto {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    // Read-only fields — present in responses, ignored in requests
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}