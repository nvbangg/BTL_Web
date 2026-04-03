package com.example.fashonshop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long productVariantId,
        @NotNull @Min(1) Integer quantity
) {
}
