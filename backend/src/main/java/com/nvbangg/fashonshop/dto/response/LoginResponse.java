package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String accessToken;
    private final Long id;
    private final String email;
    private final String name;
    private final String role;
}