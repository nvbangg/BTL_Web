package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RevenueByDayResponse {
    private final Integer day;
    private final Long revenue;
}
