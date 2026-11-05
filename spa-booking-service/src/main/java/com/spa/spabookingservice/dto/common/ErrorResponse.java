package com.spa.spabookingservice.dto.common;

import org.slf4j.MDC;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.util.TraceIdFilter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Body lỗi theo SRS 4.2:
 * { "status", "code", "message", "path", "timestamp", "traceId", "errors": [ { "field", "message" } ] }
 * errors luôn có mặt (rỗng nếu không có lỗi theo trường).
 */
public record ErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        String traceId,
        List<FieldViolation> errors
) {

    public record FieldViolation(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code, String message, String path, Map<String, String> fieldErrors) {
        List<FieldViolation> errors = fieldErrors == null ? List.of()
                : fieldErrors.entrySet().stream().map(e -> new FieldViolation(e.getKey(), e.getValue())).toList();
        return new ErrorResponse(code.getStatus().value(), code.name(), message, path, Instant.now(),
                MDC.get(TraceIdFilter.MDC_KEY), errors);
    }

    public static ErrorResponse of(ErrorCode code, String path) {
        return of(code, code.getDefaultMessage(), path, null);
    }
}
