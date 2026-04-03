package com.example.fashonshop.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminStatisticsResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalUsers,
        List<MonthlyRevenueResponse> revenueByMonth
) {
}
