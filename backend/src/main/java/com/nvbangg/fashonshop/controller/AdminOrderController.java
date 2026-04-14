package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.AdminUpdateOrderStatusRequest;
import com.nvbangg.fashonshop.dto.response.AdminOrderListResponse;
import com.nvbangg.fashonshop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<AdminOrderListResponse> getOrders(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String page,
                                                         @RequestParam(required = false) String pageSize) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", orderService.getAdminOrders(keyword, status, page, pageSize));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateOrderStatus(@PathVariable Long id,
                                               @Valid @RequestBody AdminUpdateOrderStatusRequest request) {
        orderService.updateOrderStatus(id, request);
        return ApiResponse.success("Cập nhật trạng thái đơn hàng thành công");
    }
}
