package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.AdminUpdateUserRoleRequest;
import com.nvbangg.fashonshop.dto.response.AdminUserListResponse;
import com.nvbangg.fashonshop.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String role,
                                                       @RequestParam(required = false) String page,
                                                       @RequestParam(required = false) String pageSize) {
        return ApiResponse.success("Lấy danh sách người dùng thành công", adminUserService.getUsers(keyword, role, page, pageSize));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUserRole(@PathVariable Long id,
                                            @Valid @RequestBody AdminUpdateUserRoleRequest request) {
        adminUserService.updateRole(id, request);
        return ApiResponse.success("Cập nhật vai trò thành công");
    }
}
