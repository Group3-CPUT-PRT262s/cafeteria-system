package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    // ── Retrieval methods ─────────────────────────

    // Get all active menu items (what students see)
    public List<MenuItem> getAllActiveItems() {
        return menuItemRepository.findByIsActiveTrue();
    }

    // Get active items filtered by category
    public List<MenuItem> getItemsByCategory(String category) {
        return menuItemRepository.findByIsActiveTrueAndCategory(category);
    }

    // Get a single item by ID
    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    // Get ALL items including inactive (what staff sees)
    public List<MenuItem> getAllItemsForStaff() {
        return menuItemRepository.findAll();
    }

    // ── Staff management methods ──────────────────

    // Add a new menu item
    public MenuItem addItem(MenuItem item) {
        return menuItemRepository.save(item);
    }

    // Update an existing item (name, price, description, category)
    public MenuItem updateItem(Long id, MenuItem updatedItem) {
        Optional<MenuItem> existing = menuItemRepository.findById(id);
        if (existing.isPresent()) {
            MenuItem item = existing.get();
            item.setName(updatedItem.getName());
            item.setDescription(updatedItem.getDescription());
            item.setPrice(updatedItem.getPrice());
            item.setCategory(updatedItem.getCategory());
            item.setStatus(updatedItem.getStatus());
            return menuItemRepository.save(item);
        }
        throw new RuntimeException("Menu item not found with id: " + id);
    }

    // Update status only (Available / Low Stock / Sold Out)
    public MenuItem updateStatus(Long id, String status) {
        Optional<MenuItem> existing = menuItemRepository.findById(id);
        if (existing.isPresent()) {
            MenuItem item = existing.get();
            item.setStatus(status);
            return menuItemRepository.save(item);
        }
        throw new RuntimeException("Menu item not found with id: " + id);
    }

    // Soft delete — hides item from students without removing from DB
    public void removeItem(Long id) {
        Optional<MenuItem> existing = menuItemRepository.findById(id);
        if (existing.isPresent()) {
            MenuItem item = existing.get();
            item.softDelete();
            menuItemRepository.save(item);
        } else {
            throw new RuntimeException("Menu item not found with id: " + id);
        }
    }
}