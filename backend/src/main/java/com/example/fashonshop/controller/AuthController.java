package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.dto.auth.AuthTokenResponse;
import com.example.fashonshop.dto.auth.LoginRequest;
import com.example.fashonshop.dto.auth.RefreshTokenRequest;
import com.example.fashonshop.dto.auth.RegisterRequest;
import com.example.fashonshop.dto.auth.RegisterResponse;
import com.example.fashonshop.security.CustomUserDetails;
import com.example.fashonshop.service.AuthService;
import com.example.fashonshop.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                                  HttpServletRequest servletRequest) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("REGISTER_SUCCESS", "Register successful", response,
                        servletRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                HttpServletRequest servletRequest) {
        AuthTokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("LOGIN_SUCCESS", "Login successful", response,
                servletRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                                  HttpServletRequest servletRequest) {
        AuthTokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("REFRESH_SUCCESS", "Token refreshed", response,
                servletRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@AuthenticationPrincipal CustomUserDetails principal,
                                                               HttpServletRequest servletRequest) {
        AuthUtil.requireUserId(principal);
        MessageResponse response = new MessageResponse(authService.logout());
        return ResponseEntity.ok(ApiResponse.success("LOGOUT_SUCCESS", "Logout successful", response,
                servletRequest.getRequestURI()));
    }
}
