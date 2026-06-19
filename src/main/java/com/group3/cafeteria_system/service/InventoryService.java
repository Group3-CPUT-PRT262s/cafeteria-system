package com.group3.cafeteria_system.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group3.cafeteria_system.model.Inventory;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.InventoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final MenuItemRepository menuItemRepository;

    public InventoryService(InventoryRepository inventoryRepository, MenuItemRepository menuItemRepository) {
        this.inventoryRepository = inventoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public Optional<Inventory> getInventoryByMenuItemId(Long menuItemId) {
        return inventoryRepository.findByMenuItemId(menuItemId);
    }

    public Inventory createInventory(Long menuItemId, Integer quantity,
                                     Integer reorderLevel, String unit) {
        Inventory inventory = new Inventory(menuItemId, quantity,
                reorderLevel, unit);
        return inventoryRepository.save(inventory);
    }

    // Reduces stock and syncs the MenuItem status based on
    // the new stock level. Called when an order is placed.
    public void deductStock(Long menuItemId, int quantity) {
        Inventory inventory = inventoryRepository.findByMenuItemId(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No inventory record for menu item: " + menuItemId));

        inventory.deductStock(quantity);
        inventoryRepository.save(inventory);

        // Sync MenuItem.status based on the new stock level
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found: " + menuItemId));

        item.setStatus(inventory.stockLevels());
        menuItemRepository.save(item);
    }

    // Staff restocking an item
    public Inventory restock(Long menuItemId, int additionalQuantity) {
        Inventory inventory = inventoryRepository.findByMenuItemId(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No inventory record for menu item: " + menuItemId));

        inventory.setQuantityInStock(
                inventory.getQuantityInStock() + additionalQuantity);
        Inventory saved = inventoryRepository.save(inventory);

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found: " + menuItemId));

        item.setStatus(inventory.stockLevels());
        menuItemRepository.save(item);

        return saved;
    }
}