package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterAuthResponse {
    private final String accessToken;
    private final RegisterResponse user;
}
