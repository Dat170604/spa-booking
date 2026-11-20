package com.spa.spabookingservice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        // Cho phép tối đa 10 request / 1 phút cho mỗi IP trên từng endpoint nhạy cảm
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Cập nhật đúng prefix /api/v1/... theo hệ thống của bạn
        boolean isProtectedEndpoint = path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || (path.startsWith("/api/v1/appointments") && "POST".equalsIgnoreCase(method));

        if (isProtectedEndpoint) {
            // Tránh phình RAM nếu có quá nhiều IP rác quét liên tục
            if (buckets.size() > 10_000) {
                buckets.clear();
            }

            String clientIp = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp + ":" + path, k -> createBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\": \"Bạn thao tác quá nhanh, vui lòng thử lại sau 1 phút.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}