package com.spa.spabookingservice.dto.appointment;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;

    private Long branchId;
    private String branchName;

    // Thông tin tóm tắt để hiển thị trực tiếp trên thẻ MyBookings
    private Long serviceId;
    private String serviceName;
    private Long staffId;
    private String staffName;

    // Danh sách chi tiết từng dịch vụ trong lịch hẹn
    private List<DetailItem> details;

    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalAmount;
    private BigDecimal totalPrice; // Alias để tương thích với template Angular
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class DetailItem {
        private Long detailId;
        private Long serviceId;
        private String serviceName;
        private Integer durationMinutes;
        private Long staffId;
        private String staffName;
        private BigDecimal price;
    }
}