package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.CreateOrderRequest;
import com.nvbangg.fashonshop.dto.response.CreateOrderResponse;
import com.nvbangg.fashonshop.dto.response.OrderListResponse;
import com.nvbangg.fashonshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.success("Đặt hàng thành công", orderService.createOrder(request, httpRequest));
    }

    @GetMapping("/{id}/checkout-url")
    public ApiResponse<java.util.Map<String, String>> getCheckoutUrl(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.success("Lấy link thanh toán thành công", java.util.Map.of("checkoutUrl", orderService.getCheckoutUrl(id, httpRequest)));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ApiResponse.success("Đã huỷ đơn hàng");
    }

    @GetMapping
    public ApiResponse<OrderListResponse> getMyOrders(@RequestParam(required = false) String page,
                                                      @RequestParam(required = false) String pageSize) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", orderService.getMyOrders(page, pageSize));
    }
}
