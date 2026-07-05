package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // For order history — note userId is now Long, not username String
    List<CustomerOrder> findByUserIdOrderByOrderedAtDesc(Long userId);

    // For staff dashboard filtering
    List<CustomerOrder> findByOrderStatus(String orderStatus);

    // For grouping orders by pickup slot
    List<CustomerOrder> findByTimeSlotId(Long timeSlotId);
}