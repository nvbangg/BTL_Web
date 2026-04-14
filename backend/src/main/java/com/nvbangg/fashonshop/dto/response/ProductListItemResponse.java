package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductListItemResponse {
    private final Long id;
    private final String name;
    private final String thumbnail;
    private final Long price;
}
