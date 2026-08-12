package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public TimeSlot addTimeSlot(String slotTime, Integer maxOrders) {
        if (slotTime == null || slotTime.isBlank()) {
            throw new NumberFormatException("Slot time is required.");
        }

        if (maxOrders == null || maxOrders <= 0) {
            throw new RuntimeException("Max orders must be greater than zero.");
        }

        return timeSlotRepository.save(new TimeSlot(slotTime.trim(), maxOrders));
    }

    public TimeSlot toggleSlot(Long id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Time slot not found."));
        if (slot.getIsActive()) {
            slot.deactivate();
        } else {
            slot.activate();
        }
        return timeSlotRepository.save(slot);
    }

    public void deleteSlot(Long timeSlotId) {
        TimeSlot slot = findSlotOrThrow(timeSlotId);
        timeSlotRepository.deleteById(slot);
    }

    private TimeSlot findSlotOrThrow(Long timeSlotId) {
        Objects.requireNonNull(timeSlotId, "timeSlotId must not be null");

        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException("Time slot not found."));
    }
}
