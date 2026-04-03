package com.example.fashonshop.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String description,
        String thumbnail,
        BigDecimal price,
        String gender,
        Long categoryId,
        Boolean isActive,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants
) {
}
