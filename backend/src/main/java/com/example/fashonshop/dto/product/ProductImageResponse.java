package com.example.fashonshop.dto.product;

public record ProductImageResponse(
        Long id,
        String image,
        Integer sortOrder
) {
}
