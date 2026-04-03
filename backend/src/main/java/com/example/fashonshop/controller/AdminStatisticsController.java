package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.dto.admin.AdminStatisticsResponse;
import com.example.fashonshop.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminService adminService;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics(HttpServletRequest request) {
        AdminStatisticsResponse response = adminService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_STATISTICS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }
}
