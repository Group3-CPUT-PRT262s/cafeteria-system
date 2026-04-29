package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Find all items belonging to a specific order
    List<OrderItem> findByOrderId(Long orderId);
}