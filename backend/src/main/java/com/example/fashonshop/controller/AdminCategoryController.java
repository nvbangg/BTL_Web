package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.AdminCategoryRequest;
import com.example.fashonshop.dto.admin.CategoryItemResponse;
import com.example.fashonshop.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<CategoryItemResponse>>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request
    ) {
        PageData<CategoryItemResponse> response = adminService.getCategories(keyword, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_CATEGORIES_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryItemResponse>> createCategory(@Valid @RequestBody AdminCategoryRequest requestBody,
                                                                            HttpServletRequest request) {
        CategoryItemResponse response = adminService.createCategory(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ADMIN_CREATE_CATEGORY_SUCCESS", "Success", response, request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateCategory(@PathVariable Long id,
                                                                       @Valid @RequestBody AdminCategoryRequest requestBody,
                                                                       HttpServletRequest request) {
        String message = adminService.updateCategory(id, requestBody);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_UPDATE_CATEGORY_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }
}
