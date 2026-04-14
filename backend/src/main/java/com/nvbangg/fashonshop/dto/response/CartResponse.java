package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {
    private final List<CartItemResponse> items;
    private final Long totalPrice;
}
