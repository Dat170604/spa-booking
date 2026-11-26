package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.audit.AuditLogResponse;
import com.spa.spabookingservice.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditLogService {
    void saveLog(AuditLogEntity logEntity);
    Page<AuditLogResponse> searchLogs(String keyword, String module, String status, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}