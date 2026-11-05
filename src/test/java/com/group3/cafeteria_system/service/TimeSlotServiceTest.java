package com.group3.cafeteria_system.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.CustomerOrderRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    private TimeSlotService timeSlotService;

    @BeforeEach
    void setUp() {
        timeSlotService = new TimeSlotService(timeSlotRepository, customerOrderRepository);
    }

    @Test
    void rejectsDuplicateCollectionTimes() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(new TimeSlot("12:30", 20)));

        assertThrows(IllegalArgumentException.class,
                () -> timeSlotService.addTimeSlot("12:30", 10));
    }

    @Test
    void refusesToDeleteSlotReferencedByOrders() {
        when(timeSlotRepository.findById(3L)).thenReturn(Optional.of(new TimeSlot("12:30", 20)));
        when(customerOrderRepository.existsByTimeSlotId(3L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> timeSlotService.deleteSlot(3L));
    }

    @Test
    void refusesCapacityBelowExistingOrders() {
        TimeSlot slot = new TimeSlot("12:30", 20);
        slot.setTimeSlotId(3L);
        when(timeSlotRepository.findById(3L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.findAll()).thenReturn(List.of(slot));
        when(customerOrderRepository.existsByTimeSlotId(3L)).thenReturn(true);
        when(customerOrderRepository.findByTimeSlotId(3L)).thenReturn(List.of(
                new com.group3.cafeteria_system.model.CustomerOrder(),
                new com.group3.cafeteria_system.model.CustomerOrder()));

        assertThrows(IllegalStateException.class,
                () -> timeSlotService.updateTimeSlot(3L, "12:30", 1));
    }
}
