package com.enterprise.microservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative permission check — backed by the permissions table in DB.
 *
 * Usage:
 *   @CheckPermission("PRODUCT_CREATE")
 *   public ProductDto createProduct(ProductDto dto) { ... }
 *
 * At runtime, DynamicPermissionAspect queries the database to determine
 * whether the authenticated user's roles include the required permission.
 * No hardcoded role names — fully data-driven.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {
    String value();   // permission name, e.g. "PRODUCT_READ", "USER_MANAGE"
}