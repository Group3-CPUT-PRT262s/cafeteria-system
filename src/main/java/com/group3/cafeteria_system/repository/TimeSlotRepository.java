package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // Find only active time slots for the order form dropdown (to be implemented)
    List<TimeSlot> findByIsActiveTrue();

    boolean existsBySlotTime(String slotTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TimeSlot t where t.timeSlotId = :id")
    java.util.Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}