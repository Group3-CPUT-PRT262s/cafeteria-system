package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // One order has at most one payment
    Optional<Payment> findByOrderId(Long orderId);
}