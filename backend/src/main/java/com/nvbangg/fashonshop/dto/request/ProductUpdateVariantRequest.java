package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateVariantRequest {

    private Long id;

    @NotBlank(message = "Màu phân loại là bắt buộc")
    private String color;

    @NotBlank(message = "Size phân loại là bắt buộc")
    private String size;

    @NotNull(message = "Số lượng tồn kho không hợp lệ")
    @Min(value = 0, message = "Số lượng tồn kho không hợp lệ")
    private Integer stock;

    @Min(value = 0, message = "Giá tùy chỉnh không hợp lệ")
    private Long priceOverride;
}