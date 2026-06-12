package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    List<Permission> findByResourceAndActiveTrue(String resource);

    List<Permission> findByActiveTrue();

    /**
     * Fetch all permissions assigned to a set of role names.
     * Used by CustomUserDetailsService to build the authority set.
     */
    @Query("""
            SELECT DISTINCT p FROM Permission p
            JOIN p.roles r
            WHERE r.name IN :roleNames
              AND p.active = true
            """)
    Set<Permission> findAllByRoleNames(@Param("roleNames") Set<String> roleNames);
}