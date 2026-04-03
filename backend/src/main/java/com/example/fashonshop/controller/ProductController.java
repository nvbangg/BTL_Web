package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.product.ProductDetailResponse;
import com.example.fashonshop.dto.product.ProductFiltersResponse;
import com.example.fashonshop.dto.product.ProductListItemResponse;
import com.example.fashonshop.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<ProductListItemResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "best_selling") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "16") int pageSize,
            HttpServletRequest request
    ) {
        PageData<ProductListItemResponse> response = productService.getProducts(
                keyword,
                category,
                gender,
                color,
                size,
                minPrice,
                maxPrice,
                sort,
                page,
                pageSize,
                true,
                false
        );

        return ResponseEntity.ok(ApiResponse.success("GET_PRODUCTS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<ProductFiltersResponse>> getFilters(HttpServletRequest request) {
        ProductFiltersResponse response = productService.getFilters(true);
        return ResponseEntity.ok(ApiResponse.success("GET_FILTERS_SUCCESS", "Success", response, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable Long id,
                                                                                HttpServletRequest request) {
        ProductDetailResponse response = productService.getPublicProductDetail(id);
        return ResponseEntity.ok(ApiResponse.success("GET_PRODUCT_DETAIL_SUCCESS", "Success", response,
                request.getRequestURI()));
    }
}
