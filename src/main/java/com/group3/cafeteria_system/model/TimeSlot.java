package com.group3.cafeteria_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timeSlotId; // PK value

    @Column(name = "slot_time", nullable = false)
    private String slotTime;

    // Maximum number of orders this slot can accept
    // Prevents one slot from being overwhelmed
    @Column(name = "max_orders")
    private Integer maxOrders = 20; // temp value, will discuss this later with team to determine proper slot allocation

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Constructors
    public TimeSlot() {}

    public TimeSlot(String slotTime, Integer maxOrders) {
        this.slotTime = slotTime;
        this.maxOrders = maxOrders;
        this.isActive = true;
    }

    // Business logic: switch the timeslots on/off
    public void activateTimeslot()   { this.isActive = true; }
    public void deactivateTimeslot() { this.isActive = false; }

    // Getters and Setters
    public Long getTimeSlotId() { return timeSlotId; }
    public Integer getMaxOrders() { return maxOrders; }
    public Boolean getIsActive() { return isActive; }
    public String getSlotTime() { return slotTime; }

    public void setSlotTime(String slotTime) {this.slotTime = slotTime;}
    public void setTimeSlotId(Long timeSlotId) {this.timeSlotId = timeSlotId;}
    public void setMaxOrders(Integer maxOrders) {this.maxOrders = maxOrders;}
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}