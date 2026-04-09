package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    private List<@NotNull(message = "Danh sách sản phẩm không được để trống") @Positive(message = "Danh sách sản phẩm không được để trống") Long> cartItemIds;

    @NotBlank(message = "Họ và tên người nhận không được để trống")
    private String shippingName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String shippingPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;
}
