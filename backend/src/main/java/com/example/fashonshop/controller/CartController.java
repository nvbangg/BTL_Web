package com.example.fashonshop.controller;

import com.example.fashonshop.common.ApiResponse;
import com.example.fashonshop.common.MessageResponse;
import com.example.fashonshop.dto.cart.AddCartItemRequest;
import com.example.fashonshop.dto.cart.CartResponse;
import com.example.fashonshop.dto.cart.UpdateCartItemRequest;
import com.example.fashonshop.security.CustomUserDetails;
import com.example.fashonshop.service.CartService;
import com.example.fashonshop.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> addToCart(@AuthenticationPrincipal CustomUserDetails principal,
                                                                  @Valid @RequestBody AddCartItemRequest requestBody,
                                                                  HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        String message = cartService.addToCart(userId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ADD_TO_CART_SUCCESS", "Success", new MessageResponse(message),
                        request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int pageSize,
                                                             HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        CartResponse response = cartService.getCart(userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("GET_CART_SUCCESS", "Success", response, request.getRequestURI()));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateCartItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                                       @PathVariable Long itemId,
                                                                       @Valid @RequestBody UpdateCartItemRequest requestBody,
                                                                       HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        String message = cartService.updateCartItem(userId, itemId, requestBody);
        return ResponseEntity.ok(ApiResponse.success("UPDATE_CART_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteCartItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                                       @PathVariable Long itemId,
                                                                       HttpServletRequest request) {
        Long userId = AuthUtil.requireUserId(principal);
        String message = cartService.deleteCartItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("DELETE_CART_SUCCESS", "Success", new MessageResponse(message),
                request.getRequestURI()));
    }
}
