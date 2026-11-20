package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.StaffLeaveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface StaffLeaveRepository extends JpaRepository<StaffLeaveEntity, Long> {

    // Lấy toàn bộ lịch nghỉ đã duyệt của 1 KTV trong ngày cụ thể
    List<StaffLeaveEntity> findByStaffIdAndLeaveDateAndStatus(Long staffId, LocalDate leaveDate, String status);

    // Lấy danh sách đơn nghỉ của 1 KTV (sắp xếp mới nhất)
    List<StaffLeaveEntity> findByStaffIdOrderByLeaveDateDesc(Long staffId);

    // Lấy toàn bộ đơn nghỉ theo chi nhánh (cho Admin quản lý)
    @Query("""
            SELECT l FROM StaffLeaveEntity l
            JOIN FETCH l.staff s
            WHERE (:branchId IS NULL OR s.branch.id = :branchId)
            ORDER BY l.leaveDate DESC, l.createdAt DESC
            """)
    List<StaffLeaveEntity> findAllByBranch(@Param("branchId") Long branchId);

    // Kiểm tra KTV có đang nghỉ phép trùng vào khung giờ [startTime, endTime] hay không
    @Query("""
            SELECT COUNT(l) > 0 FROM StaffLeaveEntity l
            WHERE l.staff.id = :staffId
              AND l.leaveDate = :date
              AND l.status = 'APPROVED'
              AND (
                  l.fullDay = true
                  OR (l.startTime < :endTime AND l.endTime > :startTime)
              )
            """)
    boolean isStaffOnLeave(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}