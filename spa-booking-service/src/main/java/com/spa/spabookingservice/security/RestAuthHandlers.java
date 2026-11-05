package com.spa.spabookingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import com.spa.spabookingservice.dto.common.ErrorResponse;
import com.spa.spabookingservice.exception.ErrorCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Trả 401/403 cùng format ErrorResponse cho lỗi phát sinh ở tầng filter (ngoài @RestControllerAdvice). */
@Component
@RequiredArgsConstructor
public class RestAuthHandlers {

    private final JsonMapper jsonMapper;

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> write(request, response, ErrorCode.UNAUTHORIZED);
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), ErrorResponse.of(code, request.getRequestURI()));
    }
}
