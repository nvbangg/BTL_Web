package com.example.fashonshop.repository;

import com.example.fashonshop.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductVariantId(Long userId, Long productVariantId);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    Page<CartItem> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    List<CartItem> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
