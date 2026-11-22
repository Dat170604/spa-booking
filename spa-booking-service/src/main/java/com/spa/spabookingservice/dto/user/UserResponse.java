package com.spa.spabookingservice.dto.user;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private boolean active;
    private List<String> roles;
    private LocalDateTime createdAt;
}