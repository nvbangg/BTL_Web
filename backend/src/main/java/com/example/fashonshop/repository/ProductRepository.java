package com.example.fashonshop.repository;

import com.example.fashonshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT LOWER(p.gender)
            FROM Product p
            WHERE (:onlyActive = false OR p.isActive = true)
            ORDER BY LOWER(p.gender)
            """)
    List<String> findDistinctGenders(@Param("onlyActive") boolean onlyActive);
}
