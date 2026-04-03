package com.example.fashonshop.dto.auth;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {
}
