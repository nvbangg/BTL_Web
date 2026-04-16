package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByUserIdAndProductVariantId(Long userId, Long productVariantId);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    long deleteByIdAndUserId(Long id, Long userId);
}
