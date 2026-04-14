package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.CreateOrderRequest;
import com.nvbangg.fashonshop.dto.response.CreateOrderResponse;
import com.nvbangg.fashonshop.dto.response.OrderListResponse;
import com.nvbangg.fashonshop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success("Đặt hàng thành công", orderService.createOrder(request));
    }

    @GetMapping
    public ApiResponse<OrderListResponse> getMyOrders(@RequestParam(required = false) String page,
                                                      @RequestParam(required = false) String pageSize) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", orderService.getMyOrders(page, pageSize));
    }
}
