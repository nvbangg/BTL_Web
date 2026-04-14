package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private final String accessToken;
    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final LocalDateTime createdAt;
}
