package com.enterprise.microservice.controller;

import com.enterprise.microservice.annotation.ApiLog;
import com.enterprise.microservice.annotation.CheckPermission;
import com.enterprise.microservice.dto.CreatePermissionRequest;
import com.enterprise.microservice.dto.CreateRoleRequest;
import com.enterprise.microservice.dto.PermissionResponse;
import com.enterprise.microservice.dto.RoleResponse;
import com.enterprise.microservice.service.RoleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * RoleManagementController — ADMIN-only endpoints for managing roles,
 * permissions, and user-role assignments dynamically at runtime.
 *
 * All endpoints require ROLE_ADMIN (enforced at class level via @PreAuthorize).
 * Fine-grained write operations additionally require @CheckPermission.
 *
 * Response types are always DTOs — never JPA entities (Rule #6).
 *
 * Endpoints:
 *   GET    /api/v1/admin/roles                      — list all roles
 *   POST   /api/v1/admin/roles                      — create role
 *   GET    /api/v1/admin/roles/permissions           — list all permissions
 *   POST   /api/v1/admin/roles/permissions           — create permission
 *   POST   /api/v1/admin/roles/{roleId}/permissions  — assign permissions to role
 *   DELETE /api/v1/admin/roles/{roleId}/permissions  — revoke permissions from role
 *   POST   /api/v1/admin/roles/users/{userId}/assign — assign roles to user
 *   DELETE /api/v1/admin/roles/users/{userId}/revoke — revoke roles from user
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Role Management", description = "Dynamic role and permission management (ADMIN only)")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    // -----------------------------------------------------------------------
    // Role endpoints
    // -----------------------------------------------------------------------

    @ApiLog(description = "List All Roles")
    @GetMapping
    @Operation(summary = "List all roles", description = "Returns all roles with their assigned permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(
                roleManagementService.getAllRoles().stream()
                        .map(RoleResponse::from)
                        .toList()
        );
    }

    @ApiLog(description = "Create Role", logRequestBody = true)
    @PostMapping
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Create a new role",
            description = "Creates a new role. Name is automatically prefixed with ROLE_ if not present.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "422", description = "Role already exists")
    })
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoleResponse.from(
                        roleManagementService.createRole(req.getName(), req.getDescription())));
    }

    // -----------------------------------------------------------------------
    // Permission endpoints
    // -----------------------------------------------------------------------
    @ApiLog(description = "List All Permissions")
    @GetMapping("/permissions")
    @Operation(summary = "List all permissions", description = "Returns all permissions defined in the system.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(
                roleManagementService.getAllPermissions().stream()
                        .map(PermissionResponse::from)
                        .toList()
        );
    }

    @ApiLog(description = "Create Permission", logRequestBody = true)
    @PostMapping("/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Create a new permission",
            description = "Creates a new permission. Name and resource are stored uppercase. " +
                    "Action must be one of: READ, CREATE, UPDATE, DELETE, MANAGE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Permission created successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "422", description = "Permission already exists")
    })
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PermissionResponse.from(
                        roleManagementService.createPermission(
                                req.getName(), req.getResource(), req.getAction(), req.getDescription())));
    }

    // -----------------------------------------------------------------------
    // Role ↔ Permission assignment
    // -----------------------------------------------------------------------
    @ApiLog(description = "Assign Permissions", logRequestBody = true)
    @PostMapping("/{roleId}/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Assign permissions to a role",
            description = "Adds the given permission IDs to the specified role. Existing permissions are retained.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions assigned successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    public ResponseEntity<RoleResponse> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                RoleResponse.from(roleManagementService.assignPermissionsToRole(roleId, permissionIds)));
    }

    @DeleteMapping("/{roleId}/permissions")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Revoke permissions from a role",
            description = "Removes the given permission IDs from the specified role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions revoked successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> revokePermissions(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                RoleResponse.from(roleManagementService.revokePermissionsFromRole(roleId, permissionIds)));
    }

    // -----------------------------------------------------------------------
    // User ↔ Role assignment
    // -----------------------------------------------------------------------

    @PostMapping("/users/{userId}/assign")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Assign roles to a user",
            description = "Adds the given role IDs to the specified user. Existing roles are retained.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    public ResponseEntity<Void> assignRolesToUser(
            @PathVariable Long userId,
            @RequestBody Set<Long> roleIds) {
        roleManagementService.assignRolesToUser(userId, roleIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/revoke")
    @CheckPermission("ROLE_MANAGE")
    @Operation(summary = "Revoke roles from a user",
            description = "Removes the given role IDs from the specified user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles revoked successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> revokeRolesFromUser(
            @PathVariable Long userId,
            @RequestBody Set<Long> roleIds) {
        roleManagementService.revokeRolesFromUser(userId, roleIds);
        return ResponseEntity.ok().build();
    }
}