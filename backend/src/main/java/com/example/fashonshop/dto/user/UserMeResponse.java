package com.example.fashonshop.dto.user;

public record UserMeResponse(
        Long id,
        String email,
        String name,
        String phone,
        String address,
        String role
) {
}
