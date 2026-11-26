package com.spa.spabookingservice.audit;

import com.spa.spabookingservice.entity.AuditLogEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Around("@annotation(logActivity)")
    public Object captureActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Lấy thông tin HTTP Request hiện tại
        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            request = attrs.getRequest();
        }

        String httpMethod = request != null ? request.getMethod() : null;
        String endpoint = request != null ? request.getRequestURI() : null;
        String ipAddress = extractClientIp(request);

        // Lấy thông tin người thực hiện từ SecurityContext
        String actorEmail = "ANONYMOUS";
        String actorName = "Khách vãng lai";
        String actorRoles = "";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actorEmail = auth.getName();
            actorRoles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(", "));
            actorName = userRepository.findByEmail(actorEmail)
                    .map(UserEntity::getFullName)
                    .orElse(actorEmail);
        }

        // Tự động trích xuất @PathVariable đầu tiên làm targetId (ví dụ: id lịch hẹn, id user...)
        String targetId = extractTargetId(joinPoint);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            auditLogService.saveLog(AuditLogEntity.builder()
                    .actorEmail(actorEmail)
                    .actorName(actorName)
                    .actorRoles(actorRoles)
                    .module(logActivity.module())
                    .action(logActivity.action())
                    .description(logActivity.description())
                    .targetId(targetId)
                    .httpMethod(httpMethod)
                    .endpoint(endpoint)
                    .ipAddress(ipAddress)
                    .status("SUCCESS")
                    .executionTimeMs(elapsed)
                    .build());

            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - startTime;

            auditLogService.saveLog(AuditLogEntity.builder()
                    .actorEmail(actorEmail)
                    .actorName(actorName)
                    .actorRoles(actorRoles)
                    .module(logActivity.module())
                    .action(logActivity.action())
                    .description(logActivity.description())
                    .targetId(targetId)
                    .httpMethod(httpMethod)
                    .endpoint(endpoint)
                    .ipAddress(ipAddress)
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .executionTimeMs(elapsed)
                    .build());

            throw ex;
        }
    }

    private String extractTargetId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(PathVariable.class) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}