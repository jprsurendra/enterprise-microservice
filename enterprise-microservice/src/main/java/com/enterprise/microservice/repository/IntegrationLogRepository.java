package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.IntegrationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLogEntity, Long> {

    Page<IntegrationLogEntity> findByIntegrationNameOrderByCreatedAtDesc(
            String integrationName, Pageable pageable);

    Page<IntegrationLogEntity> findBySuccessFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT i FROM IntegrationLogEntity i
            WHERE i.integrationName = :name
              AND i.success = false
              AND i.createdAt >= :since
            ORDER BY i.createdAt DESC
            """)
    Page<IntegrationLogEntity> findFailuresSince(
            @Param("name")  String name,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    long countByIntegrationNameAndSuccessAndCreatedAtAfter(
            String integrationName, boolean success, LocalDateTime after);
}