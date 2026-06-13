package com.group3.cafeteria_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
/*Note - Payment functionality proposed by Zarah
* Need to look into payment functionality, wallet implementation or scrapping the functionality
* */
@Entity
@Table(name = "payments")

public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId; //PK

    // FK to customer_orders table — one order, one payment
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid;

    // e.g. Card, Cash, EFT, Stripe
    @Column(name = "payment_method")
    private String paymentMethod;

    // Pending, Completed, Failed
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "Pending";

    // Unique reference — used for reconciliation
    // and Stripe PaymentIntent ID in Term 3
    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Constructors
    public Payment() {}

    public Payment(Long orderId, Double amountPaid, String paymentMethod) {
        this.orderId = orderId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = "Pending"; // default state, until paid/failure to pay
    }

    // Business logic methods
    // might need to add loop logic to reduce code here*
    public void markCompleted(String reference) {
        this.paymentStatus = "Completed";
        this.transactionReference = reference;
        this.paymentDate = LocalDateTime.now();
    }

    public void markFailed() {
        this.paymentStatus = "Failed";
    }

    public boolean isSuccessful() {
        return "Completed".equals(paymentStatus);
    }

    // Getters and Setters
    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public Double getAmountPaid() { return amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getTransactionReference() {return transactionReference;}
    public LocalDateTime getPaidAt() { return paymentDate; }


    public void setOrderId(Long orderId) {this.orderId = orderId;}
    public void setAmountPaid(Double amountPaid) {this.amountPaid = amountPaid;}
    public void setPaymentMethod(String paymentMethod) {this.paymentMethod = paymentMethod;}
    public void setPaymentId(Long paymentId) {this.paymentId = paymentId;}
    public void setPaymentStatus(String paymentStatus) {this.paymentStatus = paymentStatus;}
    public void setTransactionReference(String ref) {this.transactionReference = ref;}
    public void setPaidAt(LocalDateTime paidAt) {this.paymentDate = paidAt;}
}
