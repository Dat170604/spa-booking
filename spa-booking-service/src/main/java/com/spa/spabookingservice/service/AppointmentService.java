package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.dto.appointment.UpdateStatusRequest;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse createAppointment(Long customerId, AppointmentRequest request);
    AppointmentResponse getAppointmentById(Long id);
    List<AppointmentResponse> getAppointmentsByCustomer(Long customerId);
    List<AppointmentResponse> getAllAppointments();
    AppointmentResponse updateStatus(Long id, UpdateStatusRequest request);
    void cancelAppointment(Long id, Long currentUserId);
}