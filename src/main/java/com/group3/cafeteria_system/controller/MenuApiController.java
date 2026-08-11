package com.group3.cafeteria_system.controller;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuApiController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final TimeSlotRepository timeSlotRepository;

    public MenuApiController(MenuService menuService, CategoryService categoryService, TimeSlotRepository timeSlotRepository) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.timeSlotRepository = timeSlotRepository;
    }

    // ─────────────────────────────────────────
    // GET /api/menu
    // ─────────────────────────────────────────
    // DATABASE OPERATION: READ (Retrieve)
    // Queries the menu_items table for all rows
    // where is_active = true (not soft-deleted).
    // Demonstrates a SELECT with WHERE clause.
    //
    // Optional filter: ?categoryId=1
    // When provided, adds AND category_id = ?
    // to the query, demonstrating parameterised
    // database retrieval.
    //
    // Postman: GET /api/menu
    //          GET /api/menu?categoryId=1
    // ─────────────────────────────────────────
    @GetMapping("/api/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@RequestParam(required = false) Long categoryId) {

        // READ from menu_items WHERE is_active = true
        List<MenuItem> items = (categoryId != null)
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllActiveItems();

        return ResponseEntity.ok(items);
    }

    // ─────────────────────────────────────────
    // GET /api/menu/{id}
    // ─────────────────────────────────────────
    // DATABASE OPERATION: READ (single record)
    // Queries menu_items WHERE menu_item_id = ?
    // Returns 404 if the item does not exist.
    // Used by the item detail modal popup.
    //
    // Postman: GET /api/menu/1
    // ─────────────────────────────────────────
    @GetMapping("/api/menu/{id}")
    public ResponseEntity<?> getMenuItem(@PathVariable Long id) {

        // READ single record by primary key
        return menuService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────
    // GET /api/categories
    // ─────────────────────────────────────────
    // DATABASE OPERATION: READ
    // Retrieves all rows from the categories table.
    // Used to populate the filter bar on the menu page.
    //
    // Postman: GET /api/categories
    // ─────────────────────────────────────────
    @GetMapping("/api/categories")
    public ResponseEntity<List<Category>> getCategories() {

        // READ all categories from DB
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    // ─────────────────────────────────────────
    // GET /api/time-slots
    // ─────────────────────────────────────────
    // DATABASE OPERATION: READ
    // Retrieves active time slots from the
    // time_slots table WHERE is_active = true.
    // Used to populate the time slot dropdown
    // on the order/cart page.
    //
    // Postman: GET /api/time-slots
    // ─────────────────────────────────────────
    @GetMapping("/api/time-slots")
    public ResponseEntity<List<TimeSlot>> getTimeSlots() {

        // READ active slots from DB
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrue();
        return ResponseEntity.ok(slots);
    }
}