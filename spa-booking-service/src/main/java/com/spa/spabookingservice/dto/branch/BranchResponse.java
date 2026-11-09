package com.spa.spabookingservice.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String description;
    private String imageUrl;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean active;
}