package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.CartAddRequest;
import com.nvbangg.fashonshop.dto.request.CartUpdateRequest;
import com.nvbangg.fashonshop.dto.response.CartItemResponse;
import com.nvbangg.fashonshop.dto.response.CartResponse;
import com.nvbangg.fashonshop.entity.CartItem;
import com.nvbangg.fashonshop.entity.ProductVariant;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.repository.CartItemRepository;
import com.nvbangg.fashonshop.repository.ProductVariantRepository;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final JdbcTemplate jdbcTemplate;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(JdbcTemplate jdbcTemplate,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public void addItem(CartAddRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        Integer variantStock = findVariantStock(request.getProductVariantId());
        if (variantStock == null) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        CartItem existingItem = cartItemRepository
                .findByUserIdAndProductVariantId(userId, request.getProductVariantId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            validateStock(newQuantity, variantStock);

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            return;
        }

        validateStock(request.getQuantity(), variantStock);
        jdbcTemplate.update(
                "INSERT INTO cart_items(user_id, product_variant_id, quantity) VALUES (?, ?, ?)",
                userId,
                request.getProductVariantId(),
                request.getQuantity()
        );
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        Long userId = SecurityUtils.getCurrentUser().getId();

        List<CartItemResponse> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toCartItemResponse)
                .toList();

        long totalPrice = items.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        return new CartResponse(items, totalPrice);
    }

    @Transactional
    public void updateItem(Long itemId, CartUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dữ liệu yêu cầu"));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        int stock = cartItem.getProductVariant().getStock();
        validateStock(request.getQuantity(), stock);

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
    }

    public void deleteItem(Long itemId) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        long deleted = cartItemRepository.deleteByIdAndUserId(itemId, userId);
        if (deleted == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private Integer findVariantStock(Long variantId) {
        return productVariantRepository.findByIdAndProductIsActiveTrue(variantId)
                .map(ProductVariant::getStock)
                .orElse(null);
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        long price = variant.getPriceOverride() == null
                ? variant.getProduct().getPrice()
                : variant.getPriceOverride();

        return new CartItemResponse(
                item.getId(),
                variant.getProduct().getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getProduct().getThumbnail(),
                variant.getColor(),
                variant.getSize(),
                item.getQuantity(),
                price,
                variant.getStock()
        );
    }

    private void validateStock(int quantity, int stock) {
        if (quantity > stock) {
            throw new BadRequestException("Dữ liệu đầu vào không hợp lệ",
                    List.of(new ErrorDetail("quantity", "Số lượng yêu cầu vượt quá tồn kho hoặc không hợp lệ")));
        }
    }

}
