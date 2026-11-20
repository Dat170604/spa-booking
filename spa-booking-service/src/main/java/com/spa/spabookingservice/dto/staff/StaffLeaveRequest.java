package com.spa.spabookingservice.dto.staff;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class StaffLeaveRequest {
    private Long staffId; // Admin truyền vào; nếu Staff tự đăng ký thì lấy từ token

    @NotNull(message = "Ngày nghỉ không được để trống")
    private LocalDate leaveDate;

    private boolean fullDay = true;
    private LocalTime startTime; // Bắt buộc nếu fullDay = false
    private LocalTime endTime;   // Bắt buộc nếu fullDay = false
    private String reason;
}