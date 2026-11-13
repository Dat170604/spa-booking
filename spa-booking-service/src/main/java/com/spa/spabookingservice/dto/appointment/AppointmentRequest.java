package com.spa.spabookingservice.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AppointmentRequest {
    @NotNull(message = "Vui lòng chọn chi nhánh")
    private Long branchId;

    private Long serviceId;
    private List<Long> serviceIds; // Mở rộng nếu chọn nhiều dịch vụ

    private Long staffId;

    @NotNull(message = "Vui lòng chọn ngày hẹn")
    private LocalDate appointmentDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    private LocalTime startTime;

    private String notes;
}