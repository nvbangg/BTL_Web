package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndIsActiveTrue(Long id);

    long countByIsActiveTrue();
}
