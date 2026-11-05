package com.spa.spabookingservice.service;

import com.spa.spabookingservice.entity.RefreshTokenEntity;

import java.util.Optional;

public interface RefreshTokenService {

    Optional<RefreshTokenEntity> findByToken(String token);

    RefreshTokenEntity createRefreshToken(Long userId);

    RefreshTokenEntity verifyExpiration(RefreshTokenEntity token);

    void logout(String token);

    int deleteByUserId(Long userId);

}