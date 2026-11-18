package com.group3.cafeteria_system.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.Inventory;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.InventoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;
    private static final String MENU_ITEM_NOT_FOUND = "Menu item not found with id: ";

    public MenuService(MenuItemRepository menuItemRepository,
                       InventoryRepository inventoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<MenuItem> getAllActiveItems() {
        return addStockAvailability(menuItemRepository.findByIsActiveTrue());
    }

    public List<MenuItem> getItemsByCategory(Long categoryId) {
        return addStockAvailability(menuItemRepository.findByIsActiveTrueAndCategoryId(categoryId));
    }

    public List<MenuItem> getItemsByStatus(String status) {
        return addStockAvailability(menuItemRepository.findByIsActiveTrueAndStatus(status));
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id)
                .map(item -> addStockAvailability(List.of(item)).get(0));
    }

    public List<MenuItem> getAllItemsForStaff() {
        return addStockAvailability(menuItemRepository.findAll());
    }

    @Transactional
    public MenuItem addItem(MenuItem item) {
        validateStatus(item.getStatus());
        validateMaxOrderQuantity(item.getMaxOrderQuantity());
        if (item.getInitialStock() == null || item.getInitialStock() < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative.");
        }
        if (item.getInitialStock() == 0) {
            item.setStatus("Sold Out");
        }
        MenuItem saved = menuItemRepository.save(item);
        inventoryRepository.save(new Inventory(saved.getMenuItemId(),
                item.getInitialStock(), 5, "portions"));
        saved.setAvailableQuantity(item.getInitialStock());
        return saved;
    }

    @Transactional
    public MenuItem updateItem(Long id, MenuItem updatedItem) {
        validateStatus(updatedItem.getStatus());
        validateMaxOrderQuantity(updatedItem.getMaxOrderQuantity());
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MENU_ITEM_NOT_FOUND + id));
        item.setItemName(updatedItem.getItemName());
        item.setDescription(updatedItem.getDescription());
        item.setPrice(updatedItem.getPrice());
        item.setCategoryId(updatedItem.getCategoryId());
        item.setStatus(updatedItem.getStatus());
        item.setMaxOrderQuantity(updatedItem.getMaxOrderQuantity());
        if (updatedItem.getImageUrl() != null) {
            item.setImageUrl(updatedItem.getImageUrl());
        }
        return menuItemRepository.save(item);
    }

    public MenuItem updateStatus(Long id, String status) {
        validateStatus(status);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MENU_ITEM_NOT_FOUND + id));
        item.setStatus(status);
        return menuItemRepository.save(item);
    }

    private void validateStatus(String status) {
        if (status == null || !Set.of("Available", "Low Stock", "Sold Out").contains(status)) {
            throw new IllegalArgumentException("Menu status must be Available, Low Stock, or Sold Out.");
        }
    }

    private void validateMaxOrderQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Maximum order quantity must be at least 1.");
        }
    }

    private List<MenuItem> addStockAvailability(List<MenuItem> items) {
        if (items.isEmpty()) {
            return items;
        }
        Map<Long, Inventory> inventoryByItem = inventoryRepository
                .findAllByMenuItemIdIn(items.stream().map(MenuItem::getMenuItemId).toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getMenuItemId,
                        inventory -> inventory, (first, ignored) -> first));
        items.forEach(item -> {
            Inventory inventory = inventoryByItem.get(item.getMenuItemId());
            item.setAvailableQuantity(inventory == null ? 0 : inventory.getQuantityInStock());
            item.setReservedQuantity(inventory == null ? 0 : inventory.getReservedQuantity());
        });
        return items;
    }

    public void removeItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MENU_ITEM_NOT_FOUND + id));
        item.softDelete();
        menuItemRepository.save(item);
    }

    public void restoreItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MENU_ITEM_NOT_FOUND + id));
        item.restore();
        menuItemRepository.save(item);
    }
}
