package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.dto.user.ChangePasswordRequest;
import com.example.fashonshop.dto.user.UserMeResponse;
import com.example.fashonshop.security.CustomUserDetails;
import com.example.fashonshop.service.UserService;
import com.example.fashonshop.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> me(@AuthenticationPrincipal CustomUserDetails principal,
                                                          HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        UserMeResponse response = userService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success("GET_ME_SUCCESS", "Success", response, request.getRequestURI()));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                                       @Valid @RequestBody ChangePasswordRequest requestBody,
                                                                       HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        String message = userService.changePassword(userId, requestBody);
        return ResponseEntity.ok(ApiResponse.success("CHANGE_PASSWORD_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }
}
