package com.example.fashonshop.repository;

import java.math.BigDecimal;

public interface RevenueByMonthProjection {
    String getMonth();

    BigDecimal getRevenue();
}
