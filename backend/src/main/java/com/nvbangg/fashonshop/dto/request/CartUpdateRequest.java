package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartUpdateRequest {

    @NotNull(message = "Số lượng là bắt buộc")
    @Min(value = 1, message = "Số lượng yêu cầu không hợp lệ")
    private Integer quantity;
}
