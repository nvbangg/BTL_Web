package com.example.fashonshop.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductImageRequest(
        @NotBlank String image,
        @NotNull Integer sortOrder
) {
}
