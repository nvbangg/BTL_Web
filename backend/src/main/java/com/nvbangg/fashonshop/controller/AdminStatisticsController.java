package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.response.StatisticsResponse;
import com.nvbangg.fashonshop.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final OrderService orderService;

    public AdminStatisticsController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<StatisticsResponse> getStatistics() {
        return ApiResponse.success("Lấy dữ liệu thống kê thành công", orderService.getStatistics());
    }
}
