package com.nvbangg.fashonshop.repository;

import com.nvbangg.fashonshop.entity.Order;
import com.nvbangg.fashonshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);
}
