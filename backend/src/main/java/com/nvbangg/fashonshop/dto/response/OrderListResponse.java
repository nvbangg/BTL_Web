package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderListResponse {
    private final List<OrderSummaryResponse> items;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
}
