package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantRequest {

    private Long id;

    @NotBlank(message = "color là bắt buộc")
    private String color;

    @NotBlank(message = "size là bắt buộc")
    private String size;

    @NotNull(message = "stock là bắt buộc")
    @Min(value = 0, message = "stock phải lớn hơn hoặc bằng 0")
    private Integer stock;

    @Min(value = 0, message = "priceOverride phải lớn hơn hoặc bằng 0")
    private Long priceOverride;
}
