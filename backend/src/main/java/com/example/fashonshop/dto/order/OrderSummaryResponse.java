package com.example.fashonshop.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt
) {
}
