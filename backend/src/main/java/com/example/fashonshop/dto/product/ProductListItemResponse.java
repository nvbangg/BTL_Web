package com.example.fashonshop.dto.product;

import java.math.BigDecimal;

public record ProductListItemResponse(
        Long id,
        String name,
        String thumbnail,
        BigDecimal price,
        String gender,
        Long categoryId,
        Boolean isActive
) {
}
