package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.entity.RefreshTokenEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.RefreshTokenRepository;
import com.spa.spabookingservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    @DisplayName("1. Xác thực Refresh Token thành công khi chưa hết hạn và chưa bị thu hồi")
    void verifyExpiration_Success() {
        RefreshTokenEntity validToken = RefreshTokenEntity.builder()
                .token("valid_token")
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(3600).atOffset(ZoneOffset.UTC))
                .build();

        RefreshTokenEntity result = refreshTokenService.verifyExpiration(validToken);
        assertEquals("valid_token", result.getToken());
    }

    @Test
    @DisplayName("2. Xác thực Refresh Token thất bại: Token đã hết hạn -> Tự động xóa khỏi DB")
    void verifyExpiration_FailWhenExpired() {
        RefreshTokenEntity expiredToken = RefreshTokenEntity.builder()
                .token("expired_token")
                .revoked(false)
                .expiryDate(Instant.now().minusSeconds(60).atOffset(ZoneOffset.UTC))
                .build();

        AppException ex = assertThrows(AppException.class,
                () -> refreshTokenService.verifyExpiration(expiredToken));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        verify(refreshTokenRepository).delete(expiredToken);
    }
}