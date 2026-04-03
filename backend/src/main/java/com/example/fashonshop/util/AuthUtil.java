package com.example.fashonshop.util;

import com.example.fashonshop.exception.UnauthorizedException;
import com.example.fashonshop.security.CustomUserDetails;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static Long requireUserId(CustomUserDetails principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication is required");
        }
        return principal.getId();
    }
}
