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
public class ProductUpdateRequest {

    @NotBlank(message = "Tên sản phẩm là bắt buộc")
    private String name;

    private String description;

    @NotBlank(message = "Ảnh sản phẩm là bắt buộc")
    private String thumbnail;

    @NotBlank(message = "Danh mục sản phẩm là bắt buộc")
    private String category;

    @NotBlank(message = "Giới tính là bắt buộc")
    private String gender;

    @NotNull(message = "Giá sản phẩm là bắt buộc")
    @Min(value = 1, message = "Giá sản phẩm không hợp lệ")
    private Long price;

    private Boolean isActive;

    private Boolean isHot;

    @Valid
    private List<ProductUpdateImageRequest> images;

    @NotEmpty(message = "Sản phẩm phải có ít nhất một phân loại")
    @Valid
    private List<ProductUpdateVariantRequest> variants;
}