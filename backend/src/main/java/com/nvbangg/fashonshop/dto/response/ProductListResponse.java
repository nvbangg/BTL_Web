package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductListResponse {
    private final List<ProductListItemResponse> items;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
}
