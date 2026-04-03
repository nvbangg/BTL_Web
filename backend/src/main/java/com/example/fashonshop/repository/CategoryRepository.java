package com.example.fashonshop.repository;

import com.example.fashonshop.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT c FROM Category c
            WHERE :keyword IS NULL
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Category> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT DISTINCT c FROM Category c
            JOIN Product p ON p.category = c
            WHERE p.isActive = true
            ORDER BY c.name ASC
            """)
    List<Category> findAllByActiveProducts();
}
