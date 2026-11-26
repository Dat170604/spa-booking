package com.spa.spabookingservice.dto.audit;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String actorEmail;
    private String actorName;
    private String actorRoles;
    private String module;
    private String action;
    private String description;
    private String targetId;
    private String httpMethod;
    private String endpoint;
    private String ipAddress;
    private String status;
    private String errorMessage;
    private Long executionTimeMs;
    private LocalDateTime createdAt;
}