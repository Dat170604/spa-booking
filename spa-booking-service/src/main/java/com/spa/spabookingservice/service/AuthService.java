package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.auth.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String bearerToken, String refreshToken);


}