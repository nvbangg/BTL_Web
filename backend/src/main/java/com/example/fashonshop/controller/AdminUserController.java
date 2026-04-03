package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.AdminUserItemResponse;
import com.example.fashonshop.dto.admin.AdminUserRoleRequest;
import com.example.fashonshop.security.CustomUserDetails;
import com.example.fashonshop.service.AdminService;
import com.example.fashonshop.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<AdminUserItemResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request
    ) {
        PageData<AdminUserItemResponse> response = adminService.getUsers(keyword, role, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_USERS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateUserRole(@PathVariable Long id,
                                                                       @Valid @RequestBody AdminUserRoleRequest requestBody,
                                                                       HttpServletRequest request) {
        String message = adminService.updateUserRole(id, requestBody);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_UPDATE_USER_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteUser(@AuthenticationPrincipal CustomUserDetails principal,
                                                                   @PathVariable Long id,
                                                                   HttpServletRequest request) {
        Long actorUserId = AuthUtil.requireUserId(principal);
        String message = adminService.deleteUser(actorUserId, id);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_DELETE_USER_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }
}
