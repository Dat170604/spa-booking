package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.audit.AuditLogResponse;
import com.spa.spabookingservice.entity.AuditLogEntity;
import com.spa.spabookingservice.repository.AuditLogRepository;
import com.spa.spabookingservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(AuditLogEntity logEntity) {
        try {
            auditLogRepository.save(logEntity);
        } catch (Exception e) {
            // Không bao giờ để lỗi ghi log làm sập luồng nghiệp vụ chính
            log.error("Không thể lưu nhật ký hoạt động: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(String keyword, String module, String status,
                                             LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        OffsetDateTime from = (fromDate != null) ? fromDate.atStartOfDay().atOffset(offset) : null;
        OffsetDateTime to = (toDate != null) ? toDate.atTime(LocalTime.MAX).atOffset(offset) : null;

        return auditLogRepository.searchLogs(keyword, module, status, from, to, pageable)
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .actorEmail(entity.getActorEmail())
                .actorName(entity.getActorName())
                .actorRoles(entity.getActorRoles())
                .module(entity.getModule())
                .action(entity.getAction())
                .description(entity.getDescription())
                .targetId(entity.getTargetId())
                .httpMethod(entity.getHttpMethod())
                .endpoint(entity.getEndpoint())
                .ipAddress(entity.getIpAddress())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .executionTimeMs(entity.getExecutionTimeMs())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDateTime() : null)
                .build();
    }
}