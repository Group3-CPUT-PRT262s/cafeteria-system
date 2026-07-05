package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;

    public MenuService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    // Retrieval methods

    public List<MenuItem> getAllActiveItems() {
        return menuItemRepository.findByIsActiveTrue();
    }

    // Now takes a category ID instead of a category name string
    public List<MenuItem> getItemsByCategory(Long categoryId) {
        return menuItemRepository.findByIsActiveTrueAndCategoryId(categoryId);
    }

    public List<MenuItem> getItemsByStatus(String status) {
        return menuItemRepository.findByIsActiveTrueAndStatus(status);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public List<MenuItem> getAllItemsForStaff() {
        return menuItemRepository.findAll();
    }

    // Staff management methods
    // Add a new menu item
    public MenuItem addItem(MenuItem item) {
        return menuItemRepository.save(item);
    }

    // Update an existing item (name, price, description, category)
    public MenuItem updateItem(Long id, MenuItem updatedItem) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found with id: " + id));

        item.setItemName(updatedItem.getItemName());
        item.setDescription(updatedItem.getDescription());
        item.setPrice(updatedItem.getPrice());
        item.setCategoryId(updatedItem.getCategoryId());
        item.setStatus(updatedItem.getStatus());
        return menuItemRepository.save(item);
    }

    public MenuItem updateStatus(Long id, String status) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found with id: " + id));

        item.setStatus(status);
        return menuItemRepository.save(item);
    }

    public void removeItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found with id: " + id));

        item.softDelete();
        menuItemRepository.save(item);
    }

    public void restoreItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found with id: " + id));

        item.restore();
        menuItemRepository.save(item);
    }
}