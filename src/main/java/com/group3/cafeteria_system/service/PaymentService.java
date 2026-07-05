package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.Payment;
import com.group3.cafeteria_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Creates a Pending payment record when an order is placed
    public Payment createPayment(Long orderId, Double amount, String method) {
        Payment payment = new Payment(orderId, amount, method);
        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    // Called once payment is confirmed (simulated or via Stripe webhook)
    public Payment confirmPayment(Long orderId, String reference) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() ->
                        new RuntimeException("No payment found for order: " + orderId));

        payment.markCompleted(reference);
        return paymentRepository.save(payment);
    }

    public Payment failPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() ->
                        new RuntimeException("No payment found for order: " + orderId));

        payment.markFailed();
        return paymentRepository.save(payment);
    }
}