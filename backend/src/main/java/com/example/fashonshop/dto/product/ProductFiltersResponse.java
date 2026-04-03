package com.example.fashonshop.dto.product;

import com.example.fashonshop.dto.admin.CategoryItemResponse;

import java.util.List;

public record ProductFiltersResponse(
        List<CategoryItemResponse> categories,
        List<String> colors,
        List<String> sizes,
        List<String> genders
) {
}
