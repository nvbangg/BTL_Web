package com.example.fashonshop.repository;

import com.example.fashonshop.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.productVariant", "items.productVariant.product"})
    List<OrderEntity> findAllByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o")
    BigDecimal sumTotalRevenue();

    @Query(value = """
            SELECT DATE_FORMAT(o.created_at, '%Y-%m') AS month,
                   COALESCE(SUM(o.total_price), 0) AS revenue
            FROM orders o
            GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
            ORDER BY month
            """, nativeQuery = true)
    List<RevenueByMonthProjection> revenueByMonth();
}
