package com.nvbangg.fashonshop.security;

import com.nvbangg.fashonshop.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new UnauthorizedException("Chưa xác thực hoặc phiên đăng nhập hết hạn");
        }
        return user;
    }
}
