package com.group3.cafeteria_system.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId; // PK

    // FK to users table
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK to time_slots table
    @Column(name = "time_slot_id", nullable = false)
    private Long timeSlotId;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    // Lifecycle: Pending → Ready → Collected
    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @PrePersist
    protected void onCreate() {
        orderedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────
    public CustomerOrder() {}

    public CustomerOrder(Long userId, Long timeSlotId, Double totalAmount,  String orderStatus) {
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
    }

    // ── Business logic methods

    // Since we need to cycle through the order state, this method does this.
    public void advanceStatus() {
        switch (orderStatus) {
            case "Pending" -> orderStatus = "Ready";
            case "Ready"   -> orderStatus = "Collected";
            default        -> orderStatus = "Pending";
        }
    }

    public boolean isEditable() {
        return !orderStatus.equals("Collected");
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public Long getId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getTimeSlotId() { return timeSlotId; }
    public Double getTotalAmount() { return totalAmount; }
    public String getOrderStatus() { return orderStatus; }
    public LocalDateTime getOrderedAt() { return orderedAt; }

    public void setOrderId(Long orderId) {this.orderId = orderId;}
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTimeSlotId(Long timeSlotId) {this.timeSlotId = timeSlotId;}
    public void setTotalAmount(Double totalAmount) {this.totalAmount = totalAmount;}
    public void setOrderStatus(String orderStatus) {this.orderStatus = orderStatus;}
    public void setOrderedAt(LocalDateTime orderedAt) {this.orderedAt = orderedAt;}
}