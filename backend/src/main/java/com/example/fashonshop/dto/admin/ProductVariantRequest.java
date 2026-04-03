package com.example.fashonshop.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductVariantRequest(
        @NotBlank String size,
        @NotBlank String color,
        @Min(0) Integer stock,
        BigDecimal priceOverride
) {
}
