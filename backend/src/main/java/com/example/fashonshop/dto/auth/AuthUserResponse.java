package com.example.fashonshop.dto.auth;

public record AuthUserResponse(
        Long id,
        String email,
        String name,
        String role
) {
}
