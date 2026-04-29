package com.group3.cafeteria_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The display time — e.g. "12:00", "12:15", "13:30"
    @Column(name = "slot_time", nullable = false)
    private String slotTime;

    // Whether this slot is currently accepting orders.
    // Staff can deactivate a slot if it gets too full.
    @Column(name = "is_active")
    private Boolean isActive = true;

    // ── Constructors ──────────────────────────────
    public TimeSlot() {}

    public TimeSlot(String slotTime) {
        this.slotTime = slotTime;
        this.isActive = true;
    }

    // ── Business logic methods ────────────────────
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // ── Getters and Setters ───────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) {
        this.slotTime = slotTime;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}