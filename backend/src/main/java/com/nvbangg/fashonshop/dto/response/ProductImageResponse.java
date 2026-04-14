package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductImageResponse {
    private final Long id;
    private final String image;
    private final Integer sortOrder;
}
