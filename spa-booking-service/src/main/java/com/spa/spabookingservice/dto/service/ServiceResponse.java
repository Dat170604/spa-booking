package com.spa.spabookingservice.dto.service;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private Long id;
    private String name;
    private String category;
    private String imageUrl;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String status;
    private OffsetDateTime createdAt;
}