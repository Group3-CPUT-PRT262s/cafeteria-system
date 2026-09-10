package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.Inventory;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.InventoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {

    @Test
    void restockRejectsZeroQuantity() {

        InventoryRepository inventoryRepository = null;
        MenuItemRepository menuItemRepository = null;

        InventoryService service
                = new InventoryService(inventoryRepository, menuItemRepository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.restock(1L, 0)
        );
    }

    @Test
    void restockRejectsNegativeQuantity() {

        InventoryRepository inventoryRepository = null;
        MenuItemRepository menuItemRepository = null;

        InventoryService service
                = new InventoryService(inventoryRepository, menuItemRepository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.restock(1L, -5)
        );
    }

    @Test
    void restockAcceptsValidQuantity() {

        InventoryRepository inventoryRepository
                = Mockito.mock(InventoryRepository.class);

        MenuItemRepository menuItemRepository
                = Mockito.mock(MenuItemRepository.class);

        InventoryService service
                = new InventoryService(inventoryRepository, menuItemRepository);

        Inventory inventory = new Inventory();
        inventory.setQuantityInStock(20);

        MenuItem menuItem = new MenuItem();

        when(inventoryRepository.findByMenuItemId(1L))
                .thenReturn(java.util.Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        when(menuItemRepository.findById(1L))
                .thenReturn(java.util.Optional.of(menuItem));

        assertEquals(30, service.restock(1L, 10).getQuantityInStock());
    }

    @Test
    void restockUpdatesMenuItemStatus() {

        InventoryRepository inventoryRepository
                = Mockito.mock(InventoryRepository.class);

        MenuItemRepository menuItemRepository
                = Mockito.mock(MenuItemRepository.class);

        InventoryService service
                = new InventoryService(inventoryRepository, menuItemRepository);

        Inventory inventory = new Inventory();
        inventory.setQuantityInStock(0);

        MenuItem menuItem = new MenuItem();

        when(inventoryRepository.findByMenuItemId(1L))
                .thenReturn(java.util.Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        when(menuItemRepository.findById(1L))
                .thenReturn(java.util.Optional.of(menuItem));

        service.restock(1L, 10);

        Mockito.verify(menuItemRepository)
                .save(menuItem);
    }

    @Test
    void restockRejectsMissingInventory() {

        InventoryRepository inventoryRepository
                = Mockito.mock(InventoryRepository.class);

        MenuItemRepository menuItemRepository
                = Mockito.mock(MenuItemRepository.class);

        InventoryService service
                = new InventoryService(inventoryRepository, menuItemRepository);

        when(inventoryRepository.findByMenuItemId(1L))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> service.restock(1L, 10)
        );
    }
}
