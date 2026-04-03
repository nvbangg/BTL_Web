package com.example.fashonshop.dto.auth;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String email,
        String name,
        String role,
        LocalDateTime createdAt
) {
}
