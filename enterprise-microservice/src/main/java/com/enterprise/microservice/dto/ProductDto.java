package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Product data transfer object — used for both requests and responses")
public class ProductDto {

    @Schema(description = "Product database ID (read-only, ignored on create/update)", accessMode = Schema.AccessMode.READ_ONLY, example = "42")
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 100)
    @Schema(description = "Display name of the product", example = "Enterprise SSD 2TB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50)
    @Schema(description = "Stock Keeping Unit — unique identifier, immutable after creation", example = "SSD-ENT-2TB-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Size(max = 500)
    @Schema(description = "Optional product description", example = "High-performance NVMe SSD for enterprise workloads")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01")
    @Digits(integer = 17, fraction = 2)
    @Schema(description = "Unit price in USD", example = "299.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(0)
    @Schema(description = "Available stock quantity", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @NotBlank(message = "Category is required")
    @Size(max = 50)
    @Schema(description = "Product category", example = "Storage", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Schema(description = "Whether the product is active (read-only)", accessMode = Schema.AccessMode.READ_ONLY, example = "true")
    private Boolean active;

    @Schema(description = "Creation timestamp (read-only)", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp (read-only)", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}