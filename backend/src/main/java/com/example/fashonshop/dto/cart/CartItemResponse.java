package com.example.fashonshop.dto.cart;

public record CartItemResponse(
        Long id,
        Integer quantity,
        CartVariantResponse variant,
        CartProductResponse product
) {
}
