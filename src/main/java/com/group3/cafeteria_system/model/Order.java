package com.group3.cafeteria_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stores the username from the session.
    // In demo phase this is a plain string.
    // In Term 3 this becomes a proper FK to the users table.
    @Column(nullable = false)
    private String username;

    // References the time slot chosen at order placement.
    // Stored as a plain Long (the slot's ID) rather than
    // a full @ManyToOne relationship — keeps it simple
    // for the demo phase.
    @Column(name = "time_slot_id", nullable = false)
    private Long timeSlotId;

    // Order lifecycle: Pending → Ready → Collected
    @Column(nullable = false)
    private String status = "Pending";

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Automatically sets the timestamp when the
    // order is first saved to the database.
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────
    public Order() {}

    public Order(String username, Long timeSlotId, Double totalPrice) {
        this.username = username;
        this.timeSlotId = timeSlotId;
        this.totalPrice = totalPrice;
        this.status = "Pending";
    }

    // ── Business logic methods ────────────────────

    // Checks if the order can still be updated.
    // A collected order is final.
    public boolean isEditable() {
        return !status.equals("Collected");
    }

    // Moves the order to the next status in the lifecycle.
    // Pending → Ready → Collected
    public void advanceStatus() {
        switch (status) {
            case "Pending"   -> status = "Ready";
            case "Ready"     -> status = "Collected";
            default          -> {} // Already collected, no change
        }
    }

    // ── Getters and Setters ───────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Long timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}