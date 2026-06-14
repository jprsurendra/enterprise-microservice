package com.enterprise.microservice.service;

import com.enterprise.microservice.entity.Permission;
import com.enterprise.microservice.entity.Role;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.repository.PermissionRepository;
import com.enterprise.microservice.repository.RoleRepository;
import com.enterprise.microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository       userRepository;

    // -----------------------------------------------------------------------
    // Role CRUD
    // -----------------------------------------------------------------------

    @Transactional
    public Role createRole(String name, String description) {
        if (!name.startsWith("ROLE_")) {
            name = "ROLE_" + name.toUpperCase();
        }
        if (roleRepository.findByName(name).isPresent()) {
            throw new BusinessException(ErrorCode.ERR_DATA_CONFLICT,
                    "Role '" + name + "' already exists.");
        }
        Role role = new Role(name);
        role.setDescription(description);
        Role saved = roleRepository.save(role);
        log.info("Created role: {}", saved.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // -----------------------------------------------------------------------
    // Permission management
    // -----------------------------------------------------------------------

    @Transactional
    public Permission createPermission(String name, String resource,
                                       String action, String description) {
        if (permissionRepository.findByName(name).isPresent()) {
            throw new BusinessException(ErrorCode.ERR_DATA_CONFLICT,
                    "Permission '" + name + "' already exists.");
        }
        Permission p = new Permission();
        p.setName(name.toUpperCase());
        p.setResource(resource.toUpperCase());
        p.setAction(action.toUpperCase());
        p.setDescription(description);
        return permissionRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    // -----------------------------------------------------------------------
    // Assign / revoke permissions on a role
    // -----------------------------------------------------------------------
    @CacheEvict(value = "permissions", allEntries = true)   // ← evict the cache when permissions change.
    @Transactional
    public Role assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "Role not found: " + roleId));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                    "One or more permission IDs are invalid.");
        }

        role.getPermissions().addAll(permissions);
        Role updated = roleRepository.save(role);
        log.info("Assigned {} permissions to role {}", permissions.size(), role.getName());
        return updated;
    }

    @CacheEvict(value = "permissions", allEntries = true)   // ← evict the cache when permissions change.
    @Transactional
    public Role revokePermissionsFromRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "Role not found: " + roleId));

        role.getPermissions().removeIf(p -> permissionIds.contains(p.getId()));
        Role updated = roleRepository.save(role);
        log.info("Revoked permissions from role {}", role.getName());
        return updated;
    }

    // -----------------------------------------------------------------------
    // Assign / revoke roles on a user
    // -----------------------------------------------------------------------

    @Transactional
    public void assignRolesToUser(Long userId, Set<Long> roleIds) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "User not found: " + userId));

        List<Role> roles = roleRepository.findAllById(roleIds);
        user.getRoles().addAll(roles);
        userRepository.save(user);
        log.info("Assigned {} roles to user id={}", roles.size(), userId);
    }

    @Transactional
    public void revokeRolesFromUser(Long userId, Set<Long> roleIds) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "User not found: " + userId));

        user.getRoles().removeIf(r -> roleIds.contains(r.getId()));
        userRepository.save(user);
        log.info("Revoked roles from user id={}", userId);
    }

    /**
     * Returns the set of permission names granted to the given roles.
     * Cached for 60 seconds — avoids DB hit on every @CheckPermission call.
     * Cache is evicted when permissions are assigned or revoked from roles.
     */
    @Cacheable(value = "permissions", key = "#roleNames.toString()")
    @Transactional(readOnly = true)
    public Set<String> getPermissionNamesForRoles(Set<String> roleNames) {
        return permissionRepository.findAllByRoleNames(roleNames)
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

}