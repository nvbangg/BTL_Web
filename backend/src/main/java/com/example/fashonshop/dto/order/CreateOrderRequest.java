package com.example.fashonshop.dto.order;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String shippingName,
        @NotBlank String shippingPhone,
        @NotBlank String shippingAddress
) {
}
