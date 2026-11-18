package com.group3.cafeteria_system.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void reservesAndReleasesStockWithoutChangingTheReservedOrderStatus() {
        Inventory inventory = new Inventory(7L, 20, 5, "portions");

        inventory.reserveStock(5);
        assertEquals(15, inventory.getQuantityInStock());
        assertEquals(5, inventory.getReservedQuantity());

        inventory.releaseStock(5);
        assertEquals(20, inventory.getQuantityInStock());
        assertEquals(0, inventory.getReservedQuantity());
    }

    @Test
    void commitsCollectedStockWithoutRestoringAvailability() {
        Inventory inventory = new Inventory(7L, 20, 5, "portions");
        inventory.reserveStock(5);

        inventory.commitReservedStock(5);

        assertEquals(15, inventory.getQuantityInStock());
        assertEquals(0, inventory.getReservedQuantity());
    }
}
