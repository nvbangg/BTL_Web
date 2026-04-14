package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdOrderByColorAscSizeAsc(Long productId);

    Optional<ProductVariant> findByProductIdAndColorIgnoreCaseAndSizeIgnoreCase(Long productId, String color, String size);
}
