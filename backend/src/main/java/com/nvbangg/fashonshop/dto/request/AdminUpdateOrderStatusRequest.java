package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateOrderStatusRequest {

    @NotBlank(message = "Trạng thái đơn hàng là bắt buộc")
    private String status;
}
