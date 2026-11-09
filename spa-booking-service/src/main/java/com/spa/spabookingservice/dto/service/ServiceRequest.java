package com.spa.spabookingservice.dto.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 150, message = "Tên dịch vụ không vượt quá 150 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @Min(value = 0, message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 15, message = "Thời lượng tối thiểu là 15 phút")
    private Integer durationMinutes;

    private String status;
}