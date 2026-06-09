package com.enterprise.microservice.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank(message = "SKU is required")
        @Size(max = 50)
        String sku,

        @NotBlank(message = "Category is required")
        @Size(max = 50)
        String category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01")
        BigDecimal price,

        Boolean active

) {
}