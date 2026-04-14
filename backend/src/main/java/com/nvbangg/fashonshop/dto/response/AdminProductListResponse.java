package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminProductListResponse {
    private final Long totalActiveProducts;
    private final Long totalOutOfStockProducts;
    private final List<AdminProductListItemResponse> items;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
}
