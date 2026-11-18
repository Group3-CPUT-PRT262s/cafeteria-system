package com.group3.cafeteria_system.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.CustomerOrderRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;

@Service
public class TimeSlotService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TimeSlotRepository timeSlotRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository,
                           CustomerOrderRepository customerOrderRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAll();
    }

    public List<TimeSlot> getAvailableTimeSlots() {
        return timeSlotRepository.findByIsActiveTrue().stream()
                .peek(slot -> slot.setRemainingCapacity(slot.getMaxOrders() == null ? 0
                        : Math.max(0L, slot.getMaxOrders() - customerOrderRepository
                                .countActiveOrdersByTimeSlotId(slot.getTimeSlotId()))))
                .toList();
    }

    @Transactional
    public TimeSlot addTimeSlot(String slotTime, Integer maxOrders) {
        String normalizedTime = normalizeTime(slotTime);
        validateMaxOrders(maxOrders);
        ensureUniqueTime(normalizedTime, null);
        return timeSlotRepository.save(new TimeSlot(normalizedTime, maxOrders));
    }

    @Transactional
    public TimeSlot updateTimeSlot(Long id, String slotTime, Integer maxOrders) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found."));
        String normalizedTime = normalizeTime(slotTime);
        validateMaxOrders(maxOrders);
        ensureUniqueTime(normalizedTime, id);

        long orders = customerOrderRepository.countActiveOrdersByTimeSlotId(id);
        if (maxOrders < orders) {
            throw new IllegalStateException(
                    "Maximum orders cannot be lower than the " + orders
                            + " active order(s) already assigned to this slot.");
        }

        slot.setSlotTime(normalizedTime);
        slot.setMaxOrders(maxOrders);
        return timeSlotRepository.save(slot);
    }

    public Optional<TimeSlot> findById(Long timeSlotId) {
        return timeSlotRepository.findById(timeSlotId);
    }

    public TimeSlot getTimeSlot(Long timeSlotId) {
        return findSlotOrThrow(timeSlotId);
    }

    @Transactional
    public TimeSlot toggleSlot(Long id) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found."));
        if (Boolean.TRUE.equals(slot.getIsActive())) {
            slot.deactivateTimeslot();
        } else {
            slot.activateTimeslot();
        }
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public void deleteSlot(Long timeSlotId) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found."));
        if (customerOrderRepository.existsByTimeSlotId(timeSlotId)) {
            throw new IllegalStateException(
                    "This time slot has existing orders. Deactivate it to keep order history intact.");
        }
        timeSlotRepository.delete(slot);
    }

    private String normalizeTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slot time is required.");
        }
        try {
            return LocalTime.parse(value.trim()).format(TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid collection time in HH:mm format.");
        }
    }

    private void validateMaxOrders(Integer maxOrders) {
        if (maxOrders == null || maxOrders <= 0) {
            throw new IllegalArgumentException("Max orders must be greater than zero.");
        }
    }

    private void ensureUniqueTime(String time, Long exceptId) {
        LocalTime requested = LocalTime.parse(time);
        boolean duplicate = timeSlotRepository.findAll().stream()
                .filter(slot -> exceptId == null
                        || !Objects.equals(slot.getTimeSlotId(), exceptId))
                .map(TimeSlot::getSlotTime)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return LocalTime.parse(value);
                    } catch (DateTimeParseException exception) {
                        return null;
                    }
                })
                .anyMatch(requested::equals);
        if (duplicate) {
            throw new IllegalArgumentException("A collection slot already exists at that time.");
        }
    }

    private TimeSlot findSlotOrThrow(Long timeSlotId) {
        Objects.requireNonNull(timeSlotId, "timeSlotId must not be null");
        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found."));
    }
}
