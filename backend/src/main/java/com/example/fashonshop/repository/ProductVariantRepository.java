package com.example.fashonshop.repository;

import com.example.fashonshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("""
            SELECT DISTINCT LOWER(v.color)
            FROM ProductVariant v
            WHERE (:onlyActive = false OR v.product.isActive = true)
            ORDER BY LOWER(v.color)
            """)
    List<String> findDistinctColors(@Param("onlyActive") boolean onlyActive);

    @Query("""
            SELECT DISTINCT UPPER(v.size)
            FROM ProductVariant v
            WHERE (:onlyActive = false OR v.product.isActive = true)
            ORDER BY UPPER(v.size)
            """)
    List<String> findDistinctSizes(@Param("onlyActive") boolean onlyActive);
}
