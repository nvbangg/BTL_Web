package com.example.fashonshop.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryRequest(@NotBlank String name) {
}
