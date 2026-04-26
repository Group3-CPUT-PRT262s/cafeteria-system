package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // Find only active time slots for the order form dropdown
    List<TimeSlot> findByIsActiveTrue();
}