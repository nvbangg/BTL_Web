package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartAddRequest {

    @NotNull(message = "ID phân loại sản phẩm là bắt buộc và phải là số dương")
    @Positive(message = "ID phân loại sản phẩm là bắt buộc và phải là số dương")
    private Long productVariantId;

    @NotNull(message = "Số lượng yêu cầu vượt quá tồn kho hoặc không hợp lệ")
    @Min(value = 1, message = "Số lượng yêu cầu vượt quá tồn kho hoặc không hợp lệ")
    private Integer quantity;
}
