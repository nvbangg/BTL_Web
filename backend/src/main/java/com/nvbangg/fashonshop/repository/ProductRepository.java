package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByIsActiveTrue();
}
