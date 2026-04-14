package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductVariantResponse {
    private final Long id;
    private final String color;
    private final String size;
    private final Integer stock;
    private final Long priceOverride;
}
