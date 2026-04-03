package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.order.CreateOrderRequest;
import com.example.fashonshop.dto.order.OrderSummaryResponse;
import com.example.fashonshop.security.CustomUserDetails;
import com.example.fashonshop.service.OrderService;
import com.example.fashonshop.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> createOrder(@AuthenticationPrincipal CustomUserDetails principal,
                                                                         @Valid @RequestBody CreateOrderRequest requestBody,
                                                                         HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        OrderSummaryResponse response = orderService.createOrder(userId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATE_ORDER_SUCCESS", "Success", response, request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<OrderSummaryResponse>>> getOrders(@AuthenticationPrincipal CustomUserDetails principal,
                                                                                  @RequestParam(defaultValue = "1") int page,
                                                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                                                  HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        PageData<OrderSummaryResponse> response = orderService.getOrders(userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("GET_ORDERS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }
}
