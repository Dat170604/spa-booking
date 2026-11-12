package com.spa.spabookingservice.dto.service;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 150, message = "Tên dịch vụ tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Danh mục dịch vụ không được để trống")
    @Size(max = 50, message = "Danh mục tối đa 50 ký tự")
    private String category;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String imageUrl;

    private String description;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá dịch vụ phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Thời lượng dịch vụ không được để trống")
    @Min(value = 15, message = "Thời lượng tối thiểu là 15 phút")
    private Integer durationMinutes;

    @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
    private String status;
}