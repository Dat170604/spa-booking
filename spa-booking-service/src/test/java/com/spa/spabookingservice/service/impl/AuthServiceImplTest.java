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
import com.spa.spabookingservice.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity sampleUser;
    private RoleEntity customerRole;

    @BeforeEach
    void setUp() {
        customerRole = RoleEntity.builder().id(1L).name("ROLE_CUSTOMER").build();
        sampleUser = UserEntity.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("user@spa.com")
                .phone("0901234567")
                .password("encoded_pass")
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();
    }

    @Test
    @DisplayName("1. Đăng ký thành công: Mã hóa mật khẩu, gán ROLE_CUSTOMER và trả về cặp token")
    void register_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nguyễn Văn A");
        req.setEmail("user@spa.com");
        req.setPhone("0901234567");
        req.setPassword("123456");

        when(userRepository.existsByEmail("user@spa.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("123456")).thenReturn("encoded_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken("user@spa.com")).thenReturn("access_jwt");
        when(refreshTokenService.createRefreshToken(10L))
                .thenReturn(RefreshTokenEntity.builder().token("refresh_uuid").build());

        AuthResponse res = authService.register(req);

        assertEquals("access_jwt", res.getAccessToken());
        assertEquals("refresh_uuid", res.getRefreshToken());
        assertTrue(res.getRoles().contains("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("2. Đăng ký thất bại: Email đã tồn tại")
    void register_FailWhenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@spa.com");
        when(userRepository.existsByEmail("user@spa.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(req));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Đăng nhập thất bại lần thứ 5: Khóa tài khoản 10 phút trên Redis")
    void login_LockAccountOnFifthFailedAttempt() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@spa.com");
        req.setPassword("wrong_pass");

        when(redisTemplate.hasKey("account_locked:user@spa.com")).thenReturn(false);
        when(userRepository.findByEmail("user@spa.com")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("failed_attempts:user@spa.com")).thenReturn(5L);

        AppException ex = assertThrows(AppException.class, () -> authService.login(req));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(valueOperations).set("account_locked:user@spa.com", "LOCKED", 10L, TimeUnit.MINUTES);
        verify(redisTemplate).delete("failed_attempts:user@spa.com");
    }

    @Test
    @DisplayName("4. Đăng xuất: Đưa Access Token vào Redis Blacklist và revoke Refresh Token")
    void logout_RevokesBothTokens() {
        RefreshTokenEntity rt = RefreshTokenEntity.builder().token("rt_123").revoked(false).build();

        when(jwtTokenProvider.getRemainingExpirationMs("jwt_123")).thenReturn(300000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.findByToken("rt_123")).thenReturn(Optional.of(rt));

        authService.logout("Bearer jwt_123", "rt_123");

        verify(valueOperations).set("jwt:blacklist:jwt_123", "revoked", 300000L, TimeUnit.MILLISECONDS);
        assertTrue(rt.isRevoked());
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    @DisplayName("5. Refresh Token thất bại: Token đã bị đánh dấu revoked")
    void refreshToken_FailWhenRevoked() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("rt_123");

        RefreshTokenEntity revokedToken = RefreshTokenEntity.builder().token("rt_123").revoked(true).build();
        when(refreshTokenRepository.findByToken("rt_123")).thenReturn(Optional.of(revokedToken));

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(req));
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }
}