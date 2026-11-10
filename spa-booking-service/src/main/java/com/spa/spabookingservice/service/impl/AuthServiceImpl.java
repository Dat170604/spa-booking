package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.auth.*;

import com.spa.spabookingservice.entity.RefreshTokenEntity;
import com.spa.spabookingservice.entity.RoleEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.RefreshTokenRepository;
import com.spa.spabookingservice.repository.RoleRepository;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.security.JwtTokenProvider;
import com.spa.spabookingservice.service.AuthService;
import com.spa.spabookingservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 10;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra Email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được sử dụng",
                    Map.of("email", "Email đã tồn tại trên hệ thống"));
        }

        // 2. Kiểm tra Số điện thoại đã tồn tại chưa
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng",
                    Map.of("phone", "Số điện thoại đã tồn tại trên hệ thống"));
        }

        // 3. Lấy Role mặc định cho Khách hàng mới (ROLE_CUSTOMER)
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Không tìm thấy vai trò ROLE_CUSTOMER"));

        // 4. Khởi tạo đối tượng User mới
        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();

        userRepository.save(user);

        // 5. Tự động sinh Access Token & Refresh Token sau khi đăng ký thành công
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user.getId());

        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();
        String lockKey = "account_locked:" + email;
        String attemptsKey = "failed_attempts:" + email;

        // 1. Kiểm tra xem Tài khoản có đang bị khóa trên Redis không
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long expireSeconds = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            long minutesLeft = (expireSeconds != null && expireSeconds > 0) ? (expireSeconds / 60) + 1 : 1;

            throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                    String.format("Tài khoản bị tạm khóa do nhập sai 5 lần liên tiếp. Vui lòng thử lại sau %d phút.", minutesLeft));
        }

        // 2. Kiểm tra sự tồn tại của User trong Database trước
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt");
        }

        // 3. Thực hiện Xác thực Email & Password
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // --- CHỖ CẦN BỔ SUNG QUAN TRỌNG NHẤT ---
            // Tăng số lần thử sai trong Redis lên 1
            Long failedAttempts = redisTemplate.opsForValue().increment(attemptsKey);

            // Nếu là lần đầu tiên nhập sai, đặt thời gian sống cho key đếm sai (ví dụ: giữ trong 10 phút)
            if (failedAttempts != null && failedAttempts == 1) {
                redisTemplate.expire(attemptsKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            }

            // Kiểm tra nếu đã chạm ngưỡng 5 lần nhập sai
            if (failedAttempts != null && failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // Tạo key khóa tài khoản trong 10 phút
                redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
                // Xóa key đếm thất bại
                redisTemplate.delete(attemptsKey);

                throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                        String.format("Bạn đã nhập sai quá %d lần. Tài khoản tạm thời bị khóa %d phút!", MAX_FAILED_ATTEMPTS, LOCK_TIME_MINUTES));
            }

            long remainingAttempts = MAX_FAILED_ATTEMPTS - (failedAttempts != null ? failedAttempts : 0);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    String.format("Email hoặc mật khẩu không chính xác. Bạn còn %d lần thử!", remainingAttempts));
        }

        // 4. Đăng nhập thành công -> Xóa bộ đếm thất bại trên Redis
        redisTemplate.delete(attemptsKey);

        // 5. Sinh Access Token & Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user.getId());

        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshTokenEntity::getUser)
                .map(user -> {
                    String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
                    Set<String> roles = user.getRoles().stream()
                            .map(RoleEntity::getName)
                            .collect(Collectors.toSet());

                    return AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(request.getRefreshToken())
                            .userId(user.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .roles(roles)
                            .build();
                })
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
    }
}