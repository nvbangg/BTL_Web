package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductListItemResponse {
    private final Long id;
    private final String thumbnail;
    private final String name;
    private final Long price;
    private final Long soldCount;
    private final Long totalStock;
    private final Boolean isActive;
}
