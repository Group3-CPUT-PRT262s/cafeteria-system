package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // For order history — note userId is now Long, not username String
    List<CustomerOrder> findByUserIdOrderByOrderedAtDesc(Long userId);

    // For staff dashboard filtering
    List<CustomerOrder> findByOrderStatus(String orderStatus);

    List<CustomerOrder> findAllByOrderByOrderedAtAsc();

    // For grouping orders by pickup slot
    List<CustomerOrder> findByTimeSlotId(Long timeSlotId);

    boolean existsByTimeSlotId(Long timeSlotId);

    @Query("select count(o) from CustomerOrder o where o.timeSlotId = :timeSlotId "
            + "and lower(o.orderStatus) <> 'cancelled'")
    long countActiveOrdersByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderId = :id")
    java.util.Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
}