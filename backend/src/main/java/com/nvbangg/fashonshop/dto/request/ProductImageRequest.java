package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageRequest {

    private Long id;

    @NotBlank(message = "image là bắt buộc")
    private String image;

    private Integer sortOrder;
}
