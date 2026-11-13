package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AppointmentService {
    AppointmentResponse createAppointment(String userEmail, AppointmentRequest request);
    Page<AppointmentResponse> getMyAppointments(String userEmail, Pageable pageable);
    AppointmentResponse cancelAppointment(String userEmail, Long appointmentId, String reason);
    Page<AppointmentResponse> searchAppointments(String keyword, String status, Long branchId, LocalDate date, Pageable pageable);
    AppointmentResponse getById(Long id);
    AppointmentResponse updateStatus(Long id, String status);
    AppointmentResponse assignStaff(Long id, Long staffId);
    List<AppointmentResponse> getStaffBusySlots(Long staffId, LocalDate date);
    Page<AppointmentResponse> getStaffSchedule(String staffEmail, String status, LocalDate date, Pageable pageable);

}