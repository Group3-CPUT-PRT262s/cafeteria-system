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
    private final WalletService walletService;

    public CheckoutService(OrderService orderService,
                           PaymentService paymentService,
                           WalletService walletService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.walletService = walletService;
    }

    @Transactional
    public CustomerOrder checkout(Long userId, Long timeSlotId,
                                  Map<Long, Integer> cart) {
        CustomerOrder order = orderService.placeOrder(userId, timeSlotId, cart);
        walletService.debit(userId, order.getTotalAmount());

        Payment payment = paymentService.createPayment(
                order.getOrderId(), order.getTotalAmount(), "WALLET");
        paymentService.confirmPayment(
                order.getOrderId(), "WALLET-DEMO-" + payment.getPaymentId());

        return order;
    }
}
