package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.dto.request.CartAddRequest;
import com.nvbangg.fashonshop.dto.request.CartUpdateRequest;
import com.nvbangg.fashonshop.dto.response.CartResponse;
import com.nvbangg.fashonshop.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

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
