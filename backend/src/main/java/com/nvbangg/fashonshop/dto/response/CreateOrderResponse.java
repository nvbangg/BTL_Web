package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CreateOrderResponse {
    private final Long id;
    private final Long totalPrice;
    private final String status;
    private final LocalDateTime createdAt;
}
