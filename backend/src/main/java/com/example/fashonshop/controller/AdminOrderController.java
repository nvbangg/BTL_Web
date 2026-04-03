package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.AdminOrderItemResponse;
import com.example.fashonshop.dto.admin.AdminOrderStatusRequest;
import com.example.fashonshop.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<AdminOrderItemResponse>>> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request
    ) {
        PageData<AdminOrderItemResponse> response = adminService.getOrders(keyword, status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_ORDERS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateOrderStatus(@PathVariable Long id,
                                                                          @Valid @RequestBody AdminOrderStatusRequest requestBody,
                                                                          HttpServletRequest request) {
        String message = adminService.updateOrderStatus(id, requestBody);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_UPDATE_ORDER_STATUS_SUCCESS", "Success",
                new MessageResponse(message), request.getRequestURI()));
    }
}
