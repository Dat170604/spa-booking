package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.entity.*;
import com.spa.spabookingservice.repository.*;
import com.spa.spabookingservice.service.AppointmentService;
import com.spa.spabookingservice.service.EmailNotificationService;
import com.spa.spabookingservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final StaffLeaveRepository staffLeaveRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    @Override
    public AppointmentResponse createAppointment(String userEmail, AppointmentRequest request) {
        String lockKey = (request.getStaffId() != null)
                ? String.format("lock:booking:staff:%d:date:%s", request.getStaffId(), request.getAppointmentDate())
                : String.format("lock:booking:branch:%d:date:%s", request.getBranchId(), request.getAppointmentDate());

        RLock lock = redissonClient.getLock(lockKey);
        AppointmentEntity saved;

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("Hệ thống đang xử lý lịch đặt cho khung giờ này, vui lòng thử lại sau vài giây!");
            }

            // 1. Thực thi và COMMIT transaction lưu lịch hẹn trước
            saved = transactionTemplate.execute(status -> executeCreateAppointment(userEmail, request));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Quá trình đặt lịch bị gián đoạn", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        if (saved == null) {
            throw new RuntimeException("Không thể tạo lịch hẹn, vui lòng thử lại!");
        }

        // 2. Sau khi DB đã COMMIT thành công -> gửi Email & Thông báo Real-time
        emailNotificationService.sendBookingCreatedEmail(saved);

        String combinedServiceNames = saved.getDetails().stream()
                .map(d -> d.getService().getName())
                .collect(Collectors.joining(", "));
        String timeInfo = saved.getStartTime().toString().substring(0, 5) + " ngày " + saved.getAppointmentDate();
        String customerName = saved.getCustomer().getFullName();

        notificationService.sendToAllAdmins(
                "Lịch hẹn mới từ khách hàng",
                String.format("Khách %s vừa đặt dịch vụ \"%s\" lúc %s.", customerName, combinedServiceNames, timeInfo),
                "BOOKING_CREATED",
                "/admin/dashboard"
        );

        UserEntity assignedStaff = getAssignedStaff(saved);
        if (assignedStaff != null) {
            notificationService.sendToUser(
                    assignedStaff.getEmail(),
                    "Bạn có lịch hẹn mới",
                    String.format("Khách %s đã đặt lịch dịch vụ \"%s\" với bạn lúc %s.", customerName, combinedServiceNames, timeInfo),
                    "BOOKING_CREATED",
                    "/staff"
            );
        }

        return mapToResponse(saved);
    }

    private AppointmentEntity executeCreateAppointment(String userEmail, AppointmentRequest request) {
        UserEntity customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Chi nhánh không tồn tại"));

        if (!branch.isActive()) {
            throw new RuntimeException("Chi nhánh hiện đang tạm ngừng hoạt động");
        }

        List<Long> serviceIds = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            serviceIds.addAll(request.getServiceIds());
        } else if (request.getServiceId() != null) {
            serviceIds.add(request.getServiceId());
        } else {
            throw new RuntimeException("Vui lòng chọn ít nhất một dịch vụ");
        }

        List<ServiceEntity> selectedServices = serviceRepository.findAllById(serviceIds);
        if (selectedServices.isEmpty()) {
            throw new RuntimeException("Dịch vụ không tồn tại");
        }

        int totalMinutes = selectedServices.stream()
                .mapToInt(ServiceEntity::getDurationMinutes)
                .sum();
        BigDecimal totalAmount = selectedServices.stream()
                .map(ServiceEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate appointmentDate = request.getAppointmentDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(totalMinutes);

        LocalDateTime requestedDateTime = LocalDateTime.of(appointmentDate, startTime);
        LocalDateTime earliestAllowedTime = LocalDateTime.now().plusMinutes(30);

        if (requestedDateTime.isBefore(earliestAllowedTime)) {
            throw new RuntimeException("Vui lòng chọn thời gian đặt lịch cách hiện tại ít nhất 30 phút!");
        }

        if (branch.getOpenTime() != null && startTime.isBefore(branch.getOpenTime())) {
            throw new RuntimeException("Giờ đặt sớm hơn giờ mở cửa của chi nhánh (" + branch.getOpenTime() + ")");
        }
        if (branch.getCloseTime() != null && endTime.isAfter(branch.getCloseTime())) {
            throw new RuntimeException("Thời gian thực hiện vượt quá giờ đóng cửa (" + branch.getCloseTime() + ")");
        }

        UserEntity staff = null;
        if (request.getStaffId() != null) {
            staff = userRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Kỹ thuật viên không tồn tại"));

            if (appointmentRepository.existsStaffConflict(staff.getId(), request.getAppointmentDate(), startTime, endTime)) {
                throw new RuntimeException("Kỹ thuật viên này đã có lịch hẹn trong khung giờ bạn chọn!");
            }

            if (staffLeaveRepository.isStaffOnLeave(staff.getId(), appointmentDate, startTime, endTime)) {
                throw new RuntimeException("Kỹ thuật viên đang nghỉ phép trong khung giờ này. Vui lòng chọn chuyên viên hoặc khung giờ khác!");
            }
        }

        AppointmentEntity appointment = AppointmentEntity.builder()
                .customer(customer)
                .branch(branch)
                .appointmentDate(request.getAppointmentDate())
                .startTime(startTime)
                .endTime(endTime)
                .totalAmount(totalAmount)
                .status("PENDING")
                .notes(request.getNotes())
                .build();

        UserEntity assignedStaff = staff;
        List<AppointmentDetailEntity> detailList = selectedServices.stream()
                .map(svc -> AppointmentDetailEntity.builder()
                        .appointment(appointment)
                        .service(svc)
                        .staff(assignedStaff)
                        .price(svc.getPrice())
                        .build())
                .collect(Collectors.toList());

        appointment.setDetails(detailList);
        return appointmentRepository.save(appointment);
    }

    @Override
    public Page<AppointmentResponse> getMyAppointments(String userEmail, Pageable pageable) {
        UserEntity customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return appointmentRepository
                .findByCustomerIdOrderByAppointmentDateDescStartTimeDesc(customer.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(String userEmail, Long appointmentId, String reason) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"));

        if (!appointment.getCustomer().getEmail().equals(userEmail)) {
            throw new IllegalStateException("Bạn không có quyền hủy lịch hẹn này");
        }

        if (!List.of("PENDING", "CONFIRMED").contains(appointment.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy lịch hẹn đang chờ xác nhận hoặc đã xác nhận");
        }

        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getAppointmentDate(),
                appointment.getStartTime()
        );
        if (LocalDateTime.now().plusHours(24).isAfter(appointmentDateTime)) {
            throw new IllegalStateException("Bạn chỉ có thể hủy lịch hẹn trước thời gian thực hiện ít nhất 24 giờ. Vui lòng liên hệ hotline chi nhánh để được hỗ trợ!");
        }

        appointment.setStatus("CANCELLED");
        if (reason != null && !reason.isBlank()) {
            String oldNote = (appointment.getNotes() != null && !appointment.getNotes().isBlank())
                    ? appointment.getNotes() + " | "
                    : "";
            appointment.setNotes(oldNote + "Khách hủy: " + reason.trim());
        }

        AppointmentEntity saved = appointmentRepository.save(appointment);
        emailNotificationService.sendStatusChangedEmail(saved, reason);

        String timeInfo = saved.getStartTime().toString().substring(0, 5) + " ngày " + saved.getAppointmentDate();
        notificationService.sendToAllAdmins(
                "Khách hàng hủy lịch hẹn",
                String.format("Khách %s đã hủy lịch hẹn #%d lúc %s.",
                        saved.getCustomer().getFullName(), saved.getId(), timeInfo),
                "BOOKING_STATUS",
                "/admin/dashboard"
        );

        UserEntity assignedStaff = getAssignedStaff(saved);
        if (assignedStaff != null) {
            notificationService.sendToUser(
                    assignedStaff.getEmail(),
                    "Lịch hẹn đã bị hủy",
                    String.format("Khách %s đã hủy lịch hẹn lúc %s.",
                            saved.getCustomer().getFullName(), timeInfo),
                    "BOOKING_STATUS",
                    "/staff"
            );
        }

        return mapToResponse(saved);
    }

    @Override
    public Page<AppointmentResponse> searchAppointments(String keyword, String status, Long branchId, LocalDate date, Pageable pageable) {
        return appointmentRepository
                .searchAppointments(keyword, status, branchId, date, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public AppointmentResponse getById(Long id) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        return mapToResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(Long id, String status, String reason) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"));

        String upperStatus = status.toUpperCase();
        if ("CANCELLED".equals(upperStatus)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do hủy lịch hẹn!");
            }
            String oldNote = (appointment.getNotes() != null && !appointment.getNotes().isBlank())
                    ? appointment.getNotes() + " | "
                    : "";
            appointment.setNotes(oldNote + "Admin hủy: " + reason.trim());
        }

        appointment.setStatus(upperStatus);
        AppointmentEntity saved = appointmentRepository.save(appointment);
        emailNotificationService.sendStatusChangedEmail(saved, reason);

        String timeInfo = saved.getStartTime().toString().substring(0, 5) + " ngày " + saved.getAppointmentDate();
        String statusLabel = switch (upperStatus) {
            case "CONFIRMED" -> "đã được xác nhận";
            case "COMPLETED" -> "đã hoàn thành";
            case "CANCELLED" -> "đã bị hủy";
            default -> "đã cập nhật sang trạng thái " + upperStatus;
        };

        notificationService.sendToUser(
                saved.getCustomer().getEmail(),
                "Cập nhật trạng thái lịch hẹn",
                String.format("Lịch hẹn #%d lúc %s của bạn %s.", saved.getId(), timeInfo, statusLabel),
                "BOOKING_STATUS",
                "/my-bookings"
        );

        return mapToResponse(saved);
    }

    @Override
    public AppointmentResponse assignStaff(Long id, Long staffId) {
        AppointmentEntity preCheck = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        String lockKey = String.format("lock:booking:staff:%d:date:%s", staffId, preCheck.getAppointmentDate());
        RLock lock = redissonClient.getLock(lockKey);
        AppointmentEntity saved;

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("Kỹ thuật viên này đang được xử lý lịch đặt khác, vui lòng thử lại sau vài giây!");
            }

            // 1. Thực thi và COMMIT transaction phân công KTV trước
            saved = transactionTemplate.execute(status -> executeAssignStaff(id, staffId));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Quá trình phân công kỹ thuật viên bị gián đoạn", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        if (saved == null) {
            throw new RuntimeException("Không thể phân công kỹ thuật viên!");
        }

        // 2. Gửi thông báo real-time sau khi đã COMMIT DB
        UserEntity assignedStaff = getAssignedStaff(saved);
        if (assignedStaff != null) {
            String combinedServiceNames = saved.getDetails().stream()
                    .map(d -> d.getService().getName())
                    .collect(Collectors.joining(", "));
            String timeInfo = saved.getStartTime().toString().substring(0, 5) + " ngày " + saved.getAppointmentDate();

            notificationService.sendToUser(
                    assignedStaff.getEmail(),
                    "Được phân công lịch hẹn mới",
                    String.format("Quản lý đã phân công bạn phục vụ dịch vụ \"%s\" lúc %s.",
                            combinedServiceNames, timeInfo),
                    "BOOKING_STATUS",
                    "/staff"
            );
        }

        return mapToResponse(saved);
    }

    private AppointmentEntity executeAssignStaff(Long id, Long staffId) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!List.of("PENDING", "CONFIRMED").contains(appointment.getStatus())) {
            throw new IllegalStateException("Chỉ có thể phân công kỹ thuật viên cho lịch hẹn đang chờ hoặc đã xác nhận");
        }

        UserEntity staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỹ thuật viên"));

        if (staffLeaveRepository.isStaffOnLeave(
                staffId,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime())) {
            throw new RuntimeException("Kỹ thuật viên này đang nghỉ phép vào khung giờ trên");
        }

        if (appointmentRepository.existsStaffConflict(
                staffId,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime())) {
            throw new RuntimeException("Kỹ thuật viên này bị trùng lịch vào khung giờ trên");
        }

        for (AppointmentDetailEntity detail : appointment.getDetails()) {
            detail.setStaff(staff);
        }

        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getStaffBusySlots(Long staffId, LocalDate date) {
        List<AppointmentResponse> busySlots = appointmentRepository.findActiveAppointmentsByStaffAndDate(staffId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<StaffLeaveEntity> approvedLeaves = staffLeaveRepository
                .findByStaffIdAndLeaveDateAndStatus(staffId, date, "APPROVED");

        for (StaffLeaveEntity leave : approvedLeaves) {
            AppointmentResponse leaveSlot = AppointmentResponse.builder()
                    .startTime(leave.isFullDay() ? LocalTime.of(0, 0) : leave.getStartTime())
                    .endTime(leave.isFullDay() ? LocalTime.of(23, 59) : leave.getEndTime())
                    .status("LEAVE")
                    .notes(leave.getReason() != null ? "KTV nghỉ phép: " + leave.getReason() : "KTV nghỉ phép")
                    .build();
            busySlots.add(leaveSlot);
        }

        return busySlots;
    }

    @Override
    public Page<AppointmentResponse> getStaffSchedule(String staffEmail, String status, LocalDate date, Pageable pageable) {
        UserEntity staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        return appointmentRepository
                .findAppointmentsByStaffId(staff.getId(), status, date, pageable)
                .map(this::mapToResponse);
    }

    private UserEntity getAssignedStaff(AppointmentEntity appointment) {
        if (appointment.getDetails() == null || appointment.getDetails().isEmpty()) {
            return null;
        }
        return appointment.getDetails().get(0).getStaff();
    }

    private AppointmentResponse mapToResponse(AppointmentEntity a) {
        List<AppointmentResponse.DetailItem> detailItems = a.getDetails().stream()
                .map(d -> AppointmentResponse.DetailItem.builder()
                        .detailId(d.getId())
                        .serviceId(d.getService().getId())
                        .serviceName(d.getService().getName())
                        .durationMinutes(d.getService().getDurationMinutes())
                        .staffId(d.getStaff() != null ? d.getStaff().getId() : null)
                        .staffName(d.getStaff() != null ? d.getStaff().getFullName() : null)
                        .price(d.getPrice())
                        .build())
                .collect(Collectors.toList());

        String combinedServiceNames = detailItems.stream()
                .map(AppointmentResponse.DetailItem::getServiceName)
                .collect(Collectors.joining(", "));

        AppointmentResponse.DetailItem firstItem = detailItems.isEmpty() ? null : detailItems.get(0);

        return AppointmentResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer().getId())
                .customerName(a.getCustomer().getFullName())
                .customerPhone(a.getCustomer().getPhone())
                .branchId(a.getBranch().getId())
                .branchName(a.getBranch().getName())
                .serviceId(firstItem != null ? firstItem.getServiceId() : null)
                .serviceName(combinedServiceNames)
                .staffId(firstItem != null ? firstItem.getStaffId() : null)
                .staffName(firstItem != null ? firstItem.getStaffName() : null)
                .details(detailItems)
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .totalAmount(a.getTotalAmount())
                .totalPrice(a.getTotalAmount())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt().toLocalDateTime())
                .build();
    }
}