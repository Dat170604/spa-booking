package com.spa.spabookingservice.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED
}