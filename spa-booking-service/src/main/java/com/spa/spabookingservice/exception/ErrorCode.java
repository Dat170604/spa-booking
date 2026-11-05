package com.spa.spabookingservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi. Thêm mã mới theo nhóm, KHÔNG đổi mã đã phát hành.
 * HTTP status theo SRS 4.2: 401, 403, 404 (kể cả không phải thành viên dự án), 409, 422, 423, 428.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT, "Dữ liệu không hợp lệ"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ"),
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị xung đột"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "Thiếu header Idempotency-Key"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống"),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Tài khoản tạm khóa do nhập sai mật khẩu nhiều lần"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng");

    private final HttpStatus status;
    private final String defaultMessage;
}
