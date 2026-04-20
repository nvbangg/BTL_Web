package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.CartAddRequest;
import com.nvbangg.fashonshop.dto.request.CartUpdateRequest;
import com.nvbangg.fashonshop.dto.response.CartResponse;
import com.nvbangg.fashonshop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ApiResponse<Void> addToCart(@Valid @RequestBody CartAddRequest request) {
        cartService.addItem(request);
        return ApiResponse.success("Đã thêm vào giỏ hàng");
    }

    @GetMapping
    public ApiResponse<CartResponse> getMyCart() {
        return ApiResponse.success("Lấy thông tin giỏ hàng thành công", cartService.getMyCart());
    }

    @PutMapping("/{itemId}")
    public ApiResponse<Void> updateCartItem(@PathVariable Long itemId, @Valid @RequestBody CartUpdateRequest request) {
        cartService.updateItem(itemId, request);
        return ApiResponse.success("Cập nhật giỏ hàng thành công");
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> deleteCartItem(@PathVariable Long itemId) {
        cartService.deleteItem(itemId);
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng");
    }
}
