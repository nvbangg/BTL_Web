package com.nvbangg.fashonshop.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUser {
    private final Long id;
    private final String email;
    private final String role;
}
