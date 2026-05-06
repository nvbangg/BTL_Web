package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminProductDetailResponse {
    private final String name;
    private final String description;
    private final String thumbnail;
    private final String category;
    private final String gender;
    private final Long price;
    private final Boolean isActive;
    private final Boolean isHot;
    private final List<ProductImageResponse> images;
    private final List<ProductVariantResponse> variants;
}
