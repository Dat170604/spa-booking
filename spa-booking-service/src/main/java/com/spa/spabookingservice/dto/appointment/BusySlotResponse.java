package com.spa.spabookingservice.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusySlotResponse {
    private LocalTime startTime;
    private LocalTime endTime;
}