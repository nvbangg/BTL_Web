package com.example.fashonshop.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record AdminProductUpsertRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        String description,
        @NotBlank String thumbnail,
        @NotBlank String gender,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @NotNull Boolean isActive,
        List<ProductImageRequest> images,
        List<ProductVariantRequest> variants
) {
}
