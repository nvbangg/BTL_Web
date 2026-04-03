package com.example.fashonshop.dto.cart;

import java.math.BigDecimal;

public record CartVariantResponse(
        Long id,
        String size,
        String color,
        BigDecimal price,
        Integer stock
) {
}
