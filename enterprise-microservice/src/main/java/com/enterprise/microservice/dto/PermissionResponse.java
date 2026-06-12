package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Permission details returned in API responses")
public record PermissionResponse(

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        String name,
        String resource,
        String action,
        String description,
        Boolean active,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt
) {
    public static PermissionResponse from(com.enterprise.microservice.entity.Permission p) {
        return new PermissionResponse(
                p.getId(),
                p.getName(),
                p.getResource(),
                p.getAction(),
                p.getDescription(),
                p.getActive(),
                p.getCreatedAt()
        );
    }
}