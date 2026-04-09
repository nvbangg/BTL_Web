package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.ProductUpsertRequest;
import com.nvbangg.fashonshop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listProducts(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) String gender,
                                                         @RequestParam(required = false) String color,
                                                         @RequestParam(required = false) String size,
                                                         @RequestParam(required = false) Long minPrice,
                                                         @RequestParam(required = false) Long maxPrice,
                                                         @RequestParam(required = false) String sort,
                                                         @RequestParam(required = false) String isActive,
                                                         @RequestParam(required = false) String page,
                                                         @RequestParam(required = false) String pageSize) {
        return ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                productService.getAdminProducts(keyword, category, gender, color, size, minPrice, maxPrice, sort, isActive, page, pageSize)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable Long id) {
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công", productService.getAdminProductDetail(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success("Tạo sản phẩm thành công", productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequest request) {
        productService.updateProduct(id, request);
        return ApiResponse.success("Cập nhật sản phẩm thành công");
    }
}
