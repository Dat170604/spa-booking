package com.spa.spabookingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", length = 150)
    private String actorEmail;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_roles", length = 255)
    private String actorRoles;

    @Column(nullable = false, length = 50)
    private String module; // APPOINTMENT, USER, SERVICE, BRANCH, AUTH, PROFILE...

    @Column(nullable = false, length = 100)
    private String action; // CREATE_BOOKING, UPDATE_STATUS, ASSIGN_STAFF, CHANGE_PASSWORD...

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(length = 255)
    private String endpoint;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS | FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}