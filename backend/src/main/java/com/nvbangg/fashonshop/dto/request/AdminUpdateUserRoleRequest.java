package com.nvbangg.fashonshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRoleRequest {

    @NotBlank(message = "role là bắt buộc")
    private String role;
}
