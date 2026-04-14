package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private final Long id;
    private final Long productId;
    private final Long variantId;
    private final String productName;
    private final String thumbnail;
    private final String color;
    private final String size;
    private final Integer quantity;
    private final Long price;
}
