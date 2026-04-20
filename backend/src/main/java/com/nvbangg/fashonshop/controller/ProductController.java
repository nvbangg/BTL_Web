package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.response.ProductDetailResponse;
import com.nvbangg.fashonshop.dto.response.ProductFiltersResponse;
import com.nvbangg.fashonshop.dto.response.ProductListResponse;
import com.nvbangg.fashonshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<ProductListResponse> listProducts(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) String gender,
                                                         @RequestParam(required = false) String color,
                                                         @RequestParam(required = false) String size,
                                                         @RequestParam(required = false) String minPrice,
                                                         @RequestParam(required = false) String maxPrice,
                                                         @RequestParam(required = false) String sort,
                                                         @RequestParam(required = false) String page,
                                                         @RequestParam(required = false) String pageSize) {
        return ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                productService.getPublicProducts(keyword, category, gender, color, size, minPrice, maxPrice, sort, page, pageSize)
        );
    }

    @GetMapping("/filters")
    public ApiResponse<ProductFiltersResponse> getFilters() {
        return ApiResponse.success("Lấy dữ liệu bộ lọc thành công", productService.getFilters());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công", productService.getPublicProductDetail(id));
    }
}
