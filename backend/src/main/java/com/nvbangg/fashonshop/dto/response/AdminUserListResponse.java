package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminUserListResponse {
    private final Long totalPurchasedUsers;
    private final Long totalAdmins;
    private final List<AdminUserItemResponse> items;
    private final Integer page;
    private final Integer pageSize;
    private final Long total;
}
