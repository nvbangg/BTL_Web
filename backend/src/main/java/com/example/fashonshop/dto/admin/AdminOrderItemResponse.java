package com.example.fashonshop.dto.admin;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long id,
        Long userId,
        String status,
        BigDecimal totalPrice
) {
}
