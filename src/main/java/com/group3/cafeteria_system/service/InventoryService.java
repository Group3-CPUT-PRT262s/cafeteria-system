package com.group3.cafeteria_system.service;

import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.Inventory;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.InventoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import com.group3.cafeteria_system.repository.CustomerOrderRepository;
import com.group3.cafeteria_system.repository.OrderItemRepository;
import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.OrderItem;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final MenuItemRepository menuItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            MenuItemRepository menuItemRepository,
                            CustomerOrderRepository customerOrderRepository,
                            OrderItemRepository orderItemRepository) {
        this.inventoryRepository = inventoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Optional<Inventory> getInventoryByMenuItemId(Long menuItemId) {
        return inventoryRepository.findByMenuItemId(menuItemId);
    }

    @Transactional
    public Inventory createInventory(Long menuItemId, Integer quantity,
                                     Integer reorderLevel, String unit) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        Inventory inventory = new Inventory(menuItemId, quantity,
                reorderLevel, unit);
        return inventoryRepository.save(inventory);
    }

    public void reserveStock(Long menuItemId, int quantity) {
        Inventory inventory = inventoryRepository.findByMenuItemIdForUpdate(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No inventory record for menu item: " + menuItemId));

        inventory.reserveStock(quantity);
        inventoryRepository.save(inventory);
        syncMenuStatus(menuItemId, inventory);
    }

    public void releaseStock(Long menuItemId, int quantity) {
        Inventory inventory = inventoryRepository.findByMenuItemIdForUpdate(menuItemId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found for menu item: " + menuItemId));
        inventory.releaseStock(quantity);
        inventoryRepository.save(inventory);
        syncMenuStatus(menuItemId, inventory);
    }

    public void commitReservedStock(Long menuItemId, int quantity) {
        Inventory inventory = inventoryRepository.findByMenuItemIdForUpdate(menuItemId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found for menu item: " + menuItemId));
        inventory.commitReservedStock(quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void reconcileReservations(java.util.Map<Long, Integer> quantitiesByMenuItem) {
        inventoryRepository.findAll().forEach(inventory ->
                inventory.setReservedQuantity(
                        quantitiesByMenuItem.getOrDefault(inventory.getMenuItemId(), 0)));
        inventoryRepository.flush();
    }

    @Transactional
    public void ensureInventoryRecords() {
        menuItemRepository.findAll().forEach(item -> {
            if (inventoryRepository.findByMenuItemId(item.getMenuItemId()).isEmpty()) {
                inventoryRepository.save(new Inventory(item.getMenuItemId(), 0, 5, "portions"));
            }
        });
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, Integer> getOutstandingReservations() {
        java.util.Map<Long, Integer> outstanding = new java.util.HashMap<>();
        for (CustomerOrder order : customerOrderRepository.findAll()) {
            if (!"Collected".equals(order.getOrderStatus())
                    && !"Cancelled".equals(order.getOrderStatus())) {
                for (OrderItem item : orderItemRepository.findByOrderId(order.getOrderId())) {
                    outstanding.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum);
                }
            }
        }
        return outstanding;
    }

    // Staff restocking an item
    @Transactional
    public Inventory restock(Long menuItemId, int additionalQuantity) {
        validateQuantity(additionalQuantity);
        Inventory inventory = inventoryRepository.findByMenuItemIdForUpdate(menuItemId)
                .orElseGet(() -> new Inventory(menuItemId, 0, 5, "portions"));
        if (inventory.getQuantityInStock() > Integer.MAX_VALUE - additionalQuantity) {
            throw new IllegalArgumentException("Restocked quantity exceeds the supported stock limit.");
        }

        inventory.setQuantityInStock(
                inventory.getQuantityInStock() + additionalQuantity);
        Inventory saved = inventoryRepository.save(inventory);
        syncMenuStatus(menuItemId, saved);
        return saved;
    }

    private void syncMenuStatus(Long menuItemId, Inventory inventory) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found: " + menuItemId));
        item.setStatus(inventory.stockLevels());
        menuItemRepository.save(item);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }
}