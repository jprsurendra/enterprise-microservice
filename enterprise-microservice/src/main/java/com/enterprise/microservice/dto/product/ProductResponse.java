package com.enterprise.microservice.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(

        Long id,

        String name,

        String description,

        String sku,

        String category,

        BigDecimal price,

        Boolean active,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}