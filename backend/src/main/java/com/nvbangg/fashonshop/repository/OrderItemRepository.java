package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
