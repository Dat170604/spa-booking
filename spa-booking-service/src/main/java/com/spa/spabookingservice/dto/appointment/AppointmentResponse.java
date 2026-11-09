package com.spa.spabookingservice.dto.appointment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long branchId;
    private String branchName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private OffsetDateTime createdAt;
    private List<AppointmentDetailResponse> details;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentDetailResponse {
        private Long id;
        private Long serviceId;
        private String serviceName;
        private Integer durationMinutes;
        private BigDecimal price;
        private Long staffId;
        private String staffName;
    }
}