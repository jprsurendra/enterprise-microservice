package com.enterprise.microservice.aspect;

import com.enterprise.microservice.annotation.CheckPermission;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.repository.PermissionRepository;
import com.enterprise.microservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intercepts @CheckPermission annotations and dynamically verifies
 * whether the authenticated user has the required permission.
 *
 * Permission resolution:
 *   1. Extract role names from SecurityContext (already loaded by JwtAuthenticationFilter)
 *   2. Query permissions table for all permissions assigned to those roles
 *   3. Check if required permission is in that set
 *   4. Throw AccessDenied (→ 403) if not — no message hinting which permission is needed
 *
 * Caching note: in high-traffic production, add @Cacheable on
 * permissionRepository.findAllByRoleNames() with a short TTL (60s).
 * This avoids a DB query on every secured method call.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DynamicPermissionAspect {

    private final PermissionRepository permissionRepository;

    @Before("@annotation(checkPermission)")
    public void checkPermission(JoinPoint joinPoint, CheckPermission checkPermission) {
        String requiredPermission = checkPermission.value();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.ERR_AUTH_003, "Not authenticated.");
        }

        // Extract role names from the authentication object
        Set<String> roleNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        // Query DB for all permissions belonging to these roles
        Set<String> grantedPermissions = permissionRepository
                .findAllByRoleNames(roleNames)
                .stream()
                .map(p -> p.getName())
                .collect(Collectors.toSet());

        if (!grantedPermissions.contains(requiredPermission)) {
            log.warn("Permission denied — user={} required={} granted={}",
                    auth.getName(), requiredPermission, grantedPermissions);
            throw new BusinessException(ErrorCode.ERR_AUTH_004,
                    "You do not have permission to perform this action.");
        }

        log.debug("Permission granted — user={} permission={}", auth.getName(), requiredPermission);
    }
}