package com.spa.spabookingservice.controller;

import com.spa.spabookingservice.audit.LogActivity;
import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // 1. Khách hàng đặt lịch mới
    @LogActivity(module = "APPOINTMENT", action = "CREATE_APPOINTMENT", description = "Khách hàng đặt lịch hẹn mới")
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            Authentication authentication,
            @Valid @RequestBody AppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.createAppointment(authentication.getName(), request));
    }

    // 2. Khách hàng xem lịch hẹn cá nhân
    @GetMapping("/my-appointments")
    public ResponseEntity<Page<AppointmentResponse>> getMyAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
                appointmentService.getMyAppointments(authentication.getName(), PageRequest.of(page, size))
        );
    }

    // 3. Khách hàng tự hủy lịch hẹn
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(
                appointmentService.cancelAppointment(authentication.getName(), id, reason)
        );
    }

    // 4. Admin / Staff tìm kiếm & lọc toàn bộ lịch hẹn
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<AppointmentResponse>> searchAppointments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                appointmentService.searchAppointments(keyword, status, branchId, date, PageRequest.of(page, size))
        );
    }

    // 5. Xem chi tiết lịch hẹn
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    // 6. Admin cập nhật trạng thái (CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)
    @LogActivity(module = "APPOINTMENT", action = "UPDATE_APPOINTMENT_STATUS", description = "Admin cập nhật trạng thái lịch hẹn")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status, reason));
    }

    // 7. Admin phân công kỹ thuật viên
    @LogActivity(module = "APPOINTMENT", action = "ASSIGN_STAFF", description = "Admin phân công kỹ thuật viên")
    @PatchMapping("/{id}/assign-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> assignStaff(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body
    ) {
        return ResponseEntity.ok(appointmentService.assignStaff(id, body.get("staffId")));
    }

    @GetMapping("/staff-busy-slots")
    public ResponseEntity<List<AppointmentResponse>> getStaffBusySlots(
            @RequestParam Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(appointmentService.getStaffBusySlots(staffId, date));
    }

    @GetMapping("/my-schedule")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Page<AppointmentResponse>> getMySchedule(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
                appointmentService.getStaffSchedule(authentication.getName(), status, date, PageRequest.of(page, size))
        );
    }
}