package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.AppointmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    @EntityGraph(attributePaths = {"branch", "customer", "details", "details.service", "details.staff"})
    Page<AppointmentEntity> findByCustomerIdOrderByAppointmentDateDescStartTimeDesc(Long customerId, Pageable pageable);

    // Kiểm tra trùng lịch của KTV dựa trên bảng appointment_details
    @Query("""
        SELECT COUNT(d) > 0 FROM AppointmentDetailEntity d
        JOIN d.appointment a
        WHERE d.staff.id = :staffId
          AND a.appointmentDate = :date
          AND a.status NOT IN ('CANCELLED')
          AND (a.startTime < :endTime AND a.endTime > :startTime)
    """)
    boolean existsStaffConflict(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @EntityGraph(attributePaths = {"branch", "customer", "details", "details.service", "details.staff"})
    @Query("""
        SELECT a FROM AppointmentEntity a
        WHERE (:keyword IS NULL OR :keyword = '' 
               OR LOWER(a.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR a.customer.phone LIKE CONCAT('%', :keyword, '%'))
          AND (:status IS NULL OR :status = '' OR a.status = :status)
          AND (:branchId IS NULL OR a.branch.id = :branchId)
          AND (CAST(:date AS date) IS NULL OR a.appointmentDate = :date)
        ORDER BY a.appointmentDate ASC, a.startTime ASC
    """)
    Page<AppointmentEntity> searchAppointments(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch", "customer", "details", "details.service", "details.staff"})
    @Query("""
    SELECT DISTINCT a FROM AppointmentEntity a
    JOIN a.details d
    WHERE d.staff.id = :staffId
      AND a.appointmentDate = :date
      AND a.status NOT IN ('CANCELLED')
    """)
    List<AppointmentEntity> findActiveAppointmentsByStaffAndDate(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date
    );

    @EntityGraph(attributePaths = {"branch", "customer", "details", "details.service", "details.staff"})
    @Query("""
    SELECT DISTINCT a FROM AppointmentEntity a
    JOIN a.details d
    WHERE d.staff.id = :staffId
      AND (:status IS NULL OR :status = '' OR a.status = :status)
      AND (CAST(:date AS date) IS NULL OR a.appointmentDate = :date)
    ORDER BY a.appointmentDate ASC, a.startTime ASC
    """)
    Page<AppointmentEntity> findAppointmentsByStaffId(
            @Param("staffId") Long staffId,
            @Param("status") String status,
            @Param("date") LocalDate date,
            Pageable pageable
    );
}