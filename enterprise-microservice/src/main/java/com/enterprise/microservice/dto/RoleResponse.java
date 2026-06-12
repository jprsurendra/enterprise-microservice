package com.enterprise.microservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "Role details returned in API responses")
public record RoleResponse(

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        String name,
        String description,
        Set<PermissionResponse> permissions
) {
    public static RoleResponse from(com.enterprise.microservice.entity.Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions() == null ? Set.of() :
                        role.getPermissions().stream()
                                .map(PermissionResponse::from)
                                .collect(java.util.stream.Collectors.toSet())
        );
    }
}