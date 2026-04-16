package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.Order;
import com.nvbangg.fashonshop.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByStatus(OrderStatus status);
}
