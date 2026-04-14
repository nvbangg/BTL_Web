package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminOrderSummaryResponse {
    private final Long id;
    private final Long userId;
    private final String email;
    private final String shippingName;
    private final String shippingPhone;
    private final String shippingAddress;
    private final Long totalPrice;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<OrderItemResponse> orderDetails;
}
