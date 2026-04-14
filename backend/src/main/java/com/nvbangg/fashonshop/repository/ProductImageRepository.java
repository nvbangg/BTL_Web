package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    void deleteByProductId(Long productId);
}
