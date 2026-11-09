package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.dto.appointment.UpdateStatusRequest;
import com.spa.spabookingservice.entity.*;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.*;
import com.spa.spabookingservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
    public AppointmentResponse createAppointment(Long customerId, AppointmentRequest request) {
        UserEntity customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalDurationMinutes = 0;

        List<AppointmentDetailEntity> details = new ArrayList<>();

        AppointmentEntity appointment = AppointmentEntity.builder()
                .customer(customer)
                .branch(branch)
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .notes(request.getNotes())
                .status("PENDING")
                .details(details)
                .build();

        for (AppointmentRequest.AppointmentDetailRequest detailReq : request.getDetails()) {
            ServiceEntity service = serviceRepository.findById(detailReq.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

            UserEntity staff = null;
            if (detailReq.getStaffId() != null) {
                staff = userRepository.findById(detailReq.getStaffId()).orElse(null);
            }

            AppointmentDetailEntity detail = AppointmentDetailEntity.builder()
                    .appointment(appointment)
                    .service(service)
                    .staff(staff)
                    .price(service.getPrice())
                    .build();

            details.add(detail);
            totalAmount = totalAmount.add(service.getPrice());
            totalDurationMinutes += service.getDurationMinutes();
        }

        LocalTime endTime = request.getStartTime().plusMinutes(totalDurationMinutes);
        appointment.setEndTime(endTime);
        appointment.setTotalAmount(totalAmount);

        AppointmentEntity saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByCustomer(Long customerId) {
        return appointmentRepository.findByCustomerIdOrderByAppointmentDateDescStartTimeDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(Long id, UpdateStatusRequest request) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        appointment.setStatus(request.getStatus().toUpperCase());
        return mapToResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id, Long currentUserId) {
        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);
    }

    private AppointmentResponse mapToResponse(AppointmentEntity entity) {
        List<AppointmentResponse.AppointmentDetailResponse> detailResponses = entity.getDetails().stream()
                .map(d -> AppointmentResponse.AppointmentDetailResponse.builder()
                        .id(d.getId())
                        .serviceId(d.getService().getId())
                        .serviceName(d.getService().getName())
                        .durationMinutes(d.getService().getDurationMinutes())
                        .price(d.getPrice())
                        .staffId(d.getStaff() != null ? d.getStaff().getId() : null)
                        .staffName(d.getStaff() != null ? d.getStaff().getFullName() : null)
                        .build())
                .collect(Collectors.toList());

        return AppointmentResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomer().getId())
                .customerName(entity.getCustomer().getFullName())
                .customerPhone(entity.getCustomer().getPhone())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getName())
                .appointmentDate(entity.getAppointmentDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .details(detailResponses)
                .build();
    }
}