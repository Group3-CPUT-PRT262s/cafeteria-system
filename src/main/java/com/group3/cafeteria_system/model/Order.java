package com.group3.cafeteria_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, CONFIRMED, PREPARING, READY, COMPLETED, CANCELLED

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @ManyToOne
    @JoinColumn(name = "pickup_time_slot_id")
    private TimeSlot pickupTimeSlot;

    @PrePersist
    protected void onCreate() {
        orderTime = LocalDateTime.now();
    }

    public Order() {}

    public Order(User user, Double totalPrice, TimeSlot pickupTimeSlot) {
        this.user = user;
        this.totalPrice = totalPrice;
        this.pickupTimeSlot = pickupTimeSlot;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public TimeSlot getPickupTimeSlot() {
        return pickupTimeSlot;
    }

    public void setPickupTimeSlot(TimeSlot pickupTimeSlot) {
        this.pickupTimeSlot = pickupTimeSlot;
    }
}
