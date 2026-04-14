package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotEmpty(message = "Danh sách sản phẩm là bắt buộc")
    private List<@NotNull(message = "Danh sách sản phẩm là bắt buộc") Long> cartItemIds;

    @NotBlank(message = "Họ và tên người nhận là bắt buộc")
    private String shippingName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^[0-9]{9,11}$", message = "Số điện thoại không đúng định dạng")
    private String shippingPhone;

    @NotBlank(message = "Địa chỉ nhận hàng là bắt buộc")
    private String shippingAddress;
}
