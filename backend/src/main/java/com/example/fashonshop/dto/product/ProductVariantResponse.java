package com.example.fashonshop.dto.product;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        String size,
        String color,
        Integer stock,
        BigDecimal priceOverride
) {
}
