package com.spa.spabookingservice.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Exception nghiệp vụ — GlobalExceptionHandler map sang HTTP status theo ErrorCode.
 * fieldErrors (tên trường → thông điệp) được trả trong errors[], vd trùng mã trạm:
 * {@code new AppException(ErrorCode.VALIDATION_ERROR, "Mã trạm đã tồn tại", Map.of("code", "Mã trạm đã tồn tại"))}
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, String> fieldErrors;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public AppException(ErrorCode errorCode, String message, Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public AppException(ErrorCode errorCode, Map<String, String> fieldErrors) {
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }
}
