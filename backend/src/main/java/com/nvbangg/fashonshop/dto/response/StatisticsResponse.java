package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatisticsResponse {
    private final Long revenueThisMonth;
    private final Long revenueYear;
    private final Long revenueAllTime;
    private final List<RevenueByMonthResponse> revenueByMonth;
    private final List<RevenueByDayResponse> revenueByDay;
    private final List<DeliveredOrderResponse> deliveredOrders;
}
