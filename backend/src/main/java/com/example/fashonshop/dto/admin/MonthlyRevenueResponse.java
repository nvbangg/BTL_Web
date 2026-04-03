package com.example.fashonshop.dto.admin;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(
        String month,
        BigDecimal revenue
) {
}
