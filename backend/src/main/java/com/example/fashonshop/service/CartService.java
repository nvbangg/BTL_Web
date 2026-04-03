package com.example.fashonshop.service;

import com.example.fashonshop.dto.cart.AddCartItemRequest;
import com.example.fashonshop.dto.cart.CartItemResponse;
import com.example.fashonshop.dto.cart.CartProductResponse;
import com.example.fashonshop.dto.cart.CartResponse;
import com.example.fashonshop.dto.cart.CartVariantResponse;
import com.example.fashonshop.dto.cart.UpdateCartItemRequest;
import com.example.fashonshop.entity.CartItem;
import com.example.fashonshop.entity.ProductVariant;
import com.example.fashonshop.entity.User;
import com.example.fashonshop.exception.BadRequestException;
import com.example.fashonshop.exception.NotFoundException;
import com.example.fashonshop.repository.CartItemRepository;
import com.example.fashonshop.repository.ProductVariantRepository;
import com.example.fashonshop.util.PageRequestUtil;
import com.example.fashonshop.util.PriceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserService userService;

    @Transactional
    public String addToCart(Long userId, AddCartItemRequest request) {
        ProductVariant variant = getVariant(request.productVariantId());
        validateQuantity(request.quantity());

        if (variant.getStock() < request.quantity()) {
            throw new BadRequestException("OUT_OF_STOCK", "Requested quantity exceeds available stock");
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndProductVariantId(userId, request.productVariantId())
                .orElseGet(() -> {
                    User user = userService.getUser(userId);
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setProductVariant(variant);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int nextQuantity = cartItem.getQuantity() + request.quantity();
        if (variant.getStock() < nextQuantity) {
            throw new BadRequestException("OUT_OF_STOCK", "Requested quantity exceeds available stock");
        }

        cartItem.setQuantity(nextQuantity);
        cartItemRepository.save(cartItem);
        return "Added to cart";
    }

    public CartResponse getCart(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequestUtil.create(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CartItem> pagedItems = cartItemRepository.findByUserId(userId, pageable);
        List<CartItem> allItems = cartItemRepository.findAllByUserId(userId);

        BigDecimal totalPrice = allItems.stream()
                .map(item -> PriceUtil.effectiveVariantPrice(item.getProductVariant())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CartItemResponse> items = pagedItems.getContent().stream().map(this::toResponse).toList();
        return new CartResponse(items, totalPrice);
    }

    @Transactional
    public String updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        validateQuantity(request.quantity());
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));

        if (item.getProductVariant().getStock() < request.quantity()) {
            throw new BadRequestException("OUT_OF_STOCK", "Requested quantity exceeds available stock");
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return "Cart updated";
    }

    @Transactional
    public String deleteCartItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));

        cartItemRepository.delete(item);
        return "Item removed";
    }

    public List<CartItem> getAllItems(Long userId) {
        return cartItemRepository.findAllByUserId(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private ProductVariant getVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));

        if (Boolean.FALSE.equals(variant.getProduct().getIsActive())) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }

        return variant;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("INVALID_QUANTITY", "Quantity must be greater than 0");
        }
    }

    private CartItemResponse toResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal effectivePrice = PriceUtil.effectiveVariantPrice(variant);

        CartVariantResponse variantResponse = new CartVariantResponse(
                variant.getId(),
                variant.getSize(),
                variant.getColor(),
                effectivePrice,
                variant.getStock()
        );

        CartProductResponse productResponse = new CartProductResponse(
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getProduct().getThumbnail()
        );

        return new CartItemResponse(item.getId(), item.getQuantity(), variantResponse, productResponse);
    }
}
