package com.enterprise.microservice.controller;

import com.enterprise.microservice.annotation.CheckPermission;
import com.enterprise.microservice.entity.Permission;
import com.enterprise.microservice.entity.Role;
import com.enterprise.microservice.service.RoleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Role Management", description = "Dynamic role and permission management")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @GetMapping
    @Operation(summary = "List all roles with their permissions")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleManagementService.getAllRoles());
    }

    @PostMapping
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Create a new role")
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleManagementService.createRole(req.getName(), req.getDescription()));
    }

    @GetMapping("/permissions")
    @Operation(summary = "List all permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleManagementService.getAllPermissions());
    }

    @PostMapping("/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Create a new permission")
    public ResponseEntity<Permission> createPermission(@RequestBody CreatePermissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleManagementService.createPermission(
                        req.getName(), req.getResource(), req.getAction(), req.getDescription()));
    }

    @PostMapping("/{roleId}/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Assign permissions to a role")
    public ResponseEntity<Role> assignPermissions(@PathVariable Long roleId,
                                                  @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                roleManagementService.assignPermissionsToRole(roleId, permissionIds));
    }

    @DeleteMapping("/{roleId}/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Revoke permissions from a role")
    public ResponseEntity<Role> revokePermissions(@PathVariable Long roleId,
                                                  @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                roleManagementService.revokePermissionsFromRole(roleId, permissionIds));
    }

    @PostMapping("/users/{userId}/assign")
    @CheckPermission("USER_MANAGE")
    @Operation(summary = "Assign roles to a user")
    public ResponseEntity<Void> assignRolesToUser(@PathVariable Long userId,
                                                  @RequestBody Set<Long> roleIds) {
        roleManagementService.assignRolesToUser(userId, roleIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/revoke")
    @CheckPermission("USER_MANAGE")
    @Operation(summary = "Revoke roles from a user")
    public ResponseEntity<Void> revokeRolesFromUser(@PathVariable Long userId,
                                                    @RequestBody Set<Long> roleIds) {
        roleManagementService.revokeRolesFromUser(userId, roleIds);
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // Inner request DTOs
    // -----------------------------------------------------------------------

    @Data
    public static class CreateRoleRequest {
        @NotBlank private String name;
        private String description;
    }

    @Data
    public static class CreatePermissionRequest {
        @NotBlank private String name;
        @NotBlank private String resource;
        @NotBlank private String action;
        private String description;
    }
}