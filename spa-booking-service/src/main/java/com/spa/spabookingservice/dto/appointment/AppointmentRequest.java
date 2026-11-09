package com.spa.spabookingservice.dto.appointment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotNull(message = "Chi nhánh không được để trống")
    private Long branchId;

    @NotNull(message = "Ngày đặt lịch không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    private String notes;

    @NotEmpty(message = "Phải chọn ít nhất một dịch vụ")
    private List<AppointmentDetailRequest> details;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentDetailRequest {
        @NotNull(message = "Dịch vụ không được để trống")
        private Long serviceId;

        private Long staffId; // Kỹ thuật viên (có thể null)
    }
}