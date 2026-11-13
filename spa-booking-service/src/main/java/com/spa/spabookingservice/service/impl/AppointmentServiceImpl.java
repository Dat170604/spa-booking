package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.entity.*;
import com.spa.spabookingservice.repository.*;
import com.spa.spabookingservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(String userEmail, AppointmentRequest request) {
        UserEntity customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Chi nhánh không tồn tại"));

        if (!branch.isActive()) {
            throw new RuntimeException("Chi nhánh hiện đang tạm ngừng hoạt động");
        }

        // Gom danh sách ID dịch vụ (hỗ trợ cả serviceId đơn lẻ và serviceIds mảng)
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

        // Tính tổng thời lượng & tổng tiền từ danh sách dịch vụ
        int totalMinutes = selectedServices.stream()
                .mapToInt(ServiceEntity::getDurationMinutes)
                .sum();
        BigDecimal totalAmount = selectedServices.stream()
                .map(ServiceEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(totalMinutes);

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

        return mapToResponse(appointmentRepository.save(appointment));
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!appointment.getCustomer().getEmail().equals(userEmail)) {
            throw new RuntimeException("Bạn không có quyền hủy lịch hẹn này");
        }

        if (!List.of("PENDING", "CONFIRMED").contains(appointment.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy lịch hẹn đang chờ xác nhận hoặc đã xác nhận");
        }

        appointment.setStatus("CANCELLED");
        if (reason != null && !reason.isBlank()) {
            String oldNote = appointment.getNotes() != null ? appointment.getNotes() + " | " : "";
            appointment.setNotes(oldNote + "Lý do hủy: " + reason);
        }

        return mapToResponse(appointmentRepository.save(appointment));
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
    public AppointmentResponse updateStatus(Long id, String status) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        appointment.setStatus(status.toUpperCase());
        return mapToResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse assignStaff(Long id, Long staffId) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        UserEntity staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỹ thuật viên"));

        if (appointmentRepository.existsStaffConflict(staffId, appointment.getAppointmentDate(), appointment.getStartTime(), appointment.getEndTime())) {
            throw new RuntimeException("Kỹ thuật viên này bị trùng lịch vào khung giờ trên");
        }

        // Gán KTV cho các dịch vụ trong lịch hẹn
        for (AppointmentDetailEntity detail : appointment.getDetails()) {
            detail.setStaff(staff);
        }

        return mapToResponse(appointmentRepository.save(appointment));
    }

    @Override
    public List<AppointmentResponse> getStaffBusySlots(Long staffId, LocalDate date) {
        return appointmentRepository.findActiveAppointmentsByStaffAndDate(staffId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AppointmentResponse> getStaffSchedule(String staffEmail, String status, LocalDate date, Pageable pageable) {
        UserEntity staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        return appointmentRepository
                .findAppointmentsByStaffId(staff.getId(), status, date, pageable)
                .map(this::mapToResponse);
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

        // Ghép tên các dịch vụ để hiển thị gọn trên UI
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