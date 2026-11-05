package com.group3.cafeteria_system.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.Payment;

/** Coordinates the database work for the customer checkout flow. */
@Service
public class CheckoutService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public CheckoutService(OrderService orderService,
                           PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Transactional
    public CustomerOrder checkout(Long userId, Long timeSlotId,
                                  Map<Long, Integer> cart,
                                  String paymentMethod) {
        CustomerOrder order = orderService.placeOrder(userId, timeSlotId, cart);

        // This records the selected method for the current demo flow. It does
        // not charge a card or contact a payment provider.
        Payment payment = paymentService.createPayment(
                order.getOrderId(), order.getTotalAmount(), paymentMethod);
        paymentService.confirmPayment(
                order.getOrderId(), "DEMO-" + payment.getPaymentId());

        return order;
    }
}
