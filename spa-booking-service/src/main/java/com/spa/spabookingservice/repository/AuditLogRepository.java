package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("""
        SELECT a FROM AuditLogEntity a
        WHERE (:keyword IS NULL OR :keyword = '' OR
               LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(a.actorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(a.targetId) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:module IS NULL OR :module = '' OR a.module = :module)
          AND (:status IS NULL OR :status = '' OR a.status = :status)
          AND (CAST(:fromDate AS timestamp) IS NULL OR a.createdAt >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR a.createdAt <= :toDate)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLogEntity> searchLogs(
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("status") String status,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable
    );
}