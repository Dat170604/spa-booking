package com.spa.spabookingservice.dto.staff;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class StaffLeaveResponse {
    private Long id;
    private Long staffId;
    private String staffName;
    private String branchName;
    private LocalDate leaveDate;
    private boolean fullDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String status;
}