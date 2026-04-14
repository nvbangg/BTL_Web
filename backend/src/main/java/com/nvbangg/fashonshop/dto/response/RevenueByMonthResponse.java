package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RevenueByMonthResponse {
    private final String month;
    private final Long revenue;
}
