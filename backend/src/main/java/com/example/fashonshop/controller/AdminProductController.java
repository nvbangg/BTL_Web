package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.IdMessageResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.AdminProductUpsertRequest;
import com.example.fashonshop.dto.product.ProductDetailResponse;
import com.example.fashonshop.dto.product.ProductListItemResponse;
import com.example.fashonshop.service.AdminService;
import com.example.fashonshop.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final AdminService adminService;

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
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
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
                isActive,
                true
        );
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_PRODUCTS_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable Long id,
                                                                                HttpServletRequest request) {
        ProductDetailResponse response = productService.getAdminProductDetail(id);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_GET_PRODUCT_DETAIL_SUCCESS", "Success", response,
                request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IdMessageResponse>> createProduct(@Valid @RequestBody AdminProductUpsertRequest requestBody,
                                                                        HttpServletRequest request) {
        Long createdId = adminService.createProduct(requestBody);
        IdMessageResponse response = new IdMessageResponse(createdId, "Product created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ADMIN_CREATE_PRODUCT_SUCCESS", "Success", response,
                        request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateProduct(@PathVariable Long id,
                                                                      @Valid @RequestBody AdminProductUpsertRequest requestBody,
                                                                      HttpServletRequest request) {
        String message = adminService.updateProduct(id, requestBody);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_UPDATE_PRODUCT_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteProduct(@PathVariable Long id,
                                                                      HttpServletRequest request) {
        String message = adminService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("ADMIN_DELETE_PRODUCT_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }
}
