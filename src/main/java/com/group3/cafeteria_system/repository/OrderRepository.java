package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find all orders for a specific user (for order history page)
    List<Order> findByUsernameOrderByCreatedAtDesc(String username);

    // Find all orders by status (e.g. all Pending orders)
    List<Order> findByStatus(String status);

    // Find all orders for a specific time slot (for staff dashboard)
    List<Order> findByTimeSlotId(Long timeSlotId);
}