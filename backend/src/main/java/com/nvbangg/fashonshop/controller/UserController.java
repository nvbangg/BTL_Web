package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.ChangePasswordRequest;
import com.nvbangg.fashonshop.dto.request.UpdateProfileRequest;
import com.nvbangg.fashonshop.dto.response.UserResponse;
import com.nvbangg.fashonshop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success("Lấy thông tin thành công", userService.getMyProfile());
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateMyProfile(request);
        return ApiResponse.success("Cập nhật thông tin thành công");
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success("Cập nhật mật khẩu thành công");
    }
}
