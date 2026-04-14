package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminOrderListResponse {
    private final Long totalPendingOrders;
    private final Long totalIncompleteOrders;
    private final List<AdminOrderSummaryResponse> items;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
}
