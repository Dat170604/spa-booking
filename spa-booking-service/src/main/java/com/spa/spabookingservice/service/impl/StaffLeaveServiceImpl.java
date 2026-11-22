package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.staff.StaffLeaveRequest;
import com.spa.spabookingservice.dto.staff.StaffLeaveResponse;
import com.spa.spabookingservice.entity.StaffLeaveEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.repository.AppointmentRepository;
import com.spa.spabookingservice.repository.StaffLeaveRepository;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.service.NotificationService;
import com.spa.spabookingservice.service.StaffLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffLeaveServiceImpl implements StaffLeaveService {

    private final StaffLeaveRepository staffLeaveRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public StaffLeaveResponse createLeave(String callerEmail, boolean isAdmin, StaffLeaveRequest req) {
        UserEntity caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserEntity targetStaff;
        if (isAdmin && req.getStaffId() != null) {
            targetStaff = userRepository.findById(req.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kỹ thuật viên"));
        } else {
            targetStaff = caller;
        }

        if (!req.isFullDay()) {
            if (req.getStartTime() == null || req.getEndTime() == null || !req.getEndTime().isAfter(req.getStartTime())) {
                throw new RuntimeException("Giờ kết thúc nghỉ phải sau giờ bắt đầu!");
            }
        }

        // Kiểm tra xem KTV đã có lịch hẹn khách hàng trong khoảng thời gian xin nghỉ chưa
        LocalTime checkStart = req.isFullDay() ? LocalTime.MIN : req.getStartTime();
        LocalTime checkEnd = req.isFullDay() ? LocalTime.MAX : req.getEndTime();
        if (appointmentRepository.existsStaffConflict(targetStaff.getId(), req.getLeaveDate(), checkStart, checkEnd)) {
            throw new RuntimeException("Kỹ thuật viên đã có lịch hẹn với khách trong thời gian này, vui lòng chuyển lịch trước khi báo nghỉ!");
        }

        StaffLeaveEntity entity = StaffLeaveEntity.builder()
                .staff(targetStaff)
                .leaveDate(req.getLeaveDate())
                .fullDay(req.isFullDay())
                .startTime(req.isFullDay() ? null : req.getStartTime())
                .endTime(req.isFullDay() ? null : req.getEndTime())
                .reason(req.getReason())
                // Admin tạo -> APPROVED ngay; KTV tự xin -> PENDING chờ Admin duyệt
                .status(isAdmin ? "APPROVED" : "PENDING")
                .build();

        if (!isAdmin) {
            notificationService.sendToAllAdmins(
                    "Đơn xin nghỉ phép mới",
                    targetStaff.getFullName() + " vừa gửi đơn xin nghỉ ngày " + req.getLeaveDate(),
                    "LEAVE_REQUEST",
                    "/admin/staff-leaves"
            );
        }

        return toResponse(staffLeaveRepository.save(entity));
    }

    @Transactional
    public StaffLeaveResponse updateStatus(Long leaveId, String status) {
        StaffLeaveEntity entity = staffLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));
        entity.setStatus(status.toUpperCase());

        String statusText = "APPROVED".equals(entity.getStatus()) ? "được phê duyệt" : "bị từ chối";
        notificationService.sendToUser(
                entity.getStaff().getEmail(),
                "Cập nhật đơn nghỉ phép",
                "Đơn xin nghỉ ngày " + entity.getLeaveDate() + " của bạn đã " + statusText + ".",
                "LEAVE_STATUS",
                "/staff/leaves"
        );

        return toResponse(staffLeaveRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<StaffLeaveResponse> getMyLeaves(String email) {
        UserEntity staff = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return staffLeaveRepository.findByStaffIdOrderByLeaveDateDesc(staff.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffLeaveResponse> getAllLeaves(Long branchId) {
        return staffLeaveRepository.findAllByBranch(branchId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteLeave(Long leaveId) {
        staffLeaveRepository.deleteById(leaveId);
    }

    private StaffLeaveResponse toResponse(StaffLeaveEntity e) {
        return StaffLeaveResponse.builder()
                .id(e.getId())
                .staffId(e.getStaff().getId())
                .staffName(e.getStaff().getFullName())
                .branchName(e.getStaff().getBranch() != null ? e.getStaff().getBranch().getName() : null)
                .leaveDate(e.getLeaveDate())
                .fullDay(e.isFullDay())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .reason(e.getReason())
                .status(e.getStatus())
                .build();
    }
}