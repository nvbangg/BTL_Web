package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductUpsertRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Ảnh sản phẩm không được để trống")
    private String thumbnail;

    @NotBlank(message = "Danh mục sản phẩm không được để trống")
    private String category;

    @NotBlank(message = "Giới tính không hợp lệ (Hỗ trợ: male, female, unisex)")
    private String gender;

    @NotNull(message = "Giá sản phẩm phải là số nguyên dương")
    @Min(value = 1, message = "Giá sản phẩm phải là số nguyên dương")
    private Long price;

    private Boolean isActive;

    @Valid
    private List<ProductImageRequest> images;

    @NotEmpty(message = "Sản phẩm phải có ít nhất một phân loại (màu sắc/kích thước)")
    @Valid
    private List<ProductVariantRequest> variants;
}
