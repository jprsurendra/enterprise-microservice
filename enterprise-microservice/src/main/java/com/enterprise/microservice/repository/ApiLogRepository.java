package com.enterprise.microservice.repository;

import com.enterprise.microservice.entity.ApiLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLogEntity, Long> {

    Page<ApiLogEntity> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<ApiLogEntity> findByEndpointContainingOrderByCreatedAtDesc(String endpoint, Pageable pageable);

    @Query("""
            SELECT a FROM ApiLogEntity a
            WHERE a.createdAt BETWEEN :from AND :to
            ORDER BY a.createdAt DESC
            """)
    Page<ApiLogEntity> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);

    @Query("""
            SELECT a.endpoint, COUNT(a), AVG(a.executionMs)
            FROM ApiLogEntity a
            WHERE a.createdAt >= :since
            GROUP BY a.endpoint
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> findTopEndpointsSince(@Param("since") LocalDateTime since);

    long countByHttpStatusGreaterThanEqualAndCreatedAtAfter(int status, LocalDateTime after);
}