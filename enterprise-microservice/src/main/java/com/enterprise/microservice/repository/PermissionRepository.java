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
     * Fetch all active permissions assigned to a given set of role names.
     *
     * Navigates FROM Role → permissions (the owning side of the join table).
     * Permission entity has no 'roles' field — querying p.roles would fail
     * with UnknownPathException since that attribute does not exist.
     */
    @Query("""
            SELECT DISTINCT p FROM Role r
            JOIN r.permissions p
            WHERE r.name IN :roleNames
              AND p.active = true
            """)
    Set<Permission> findAllByRoleNames(@Param("roleNames") Set<String> roleNames);
}