package com.group3.cafeteria_system.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import com.group3.cafeteria_system.service.OrderService;
import com.group3.cafeteria_system.service.TimeSlotService;
import com.group3.cafeteria_system.service.UserService;

@Controller
public class StaffController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final TimeSlotService timeSlotService;
    private final UserService userService;

    public StaffController(MenuService menuService, CategoryService categoryService, OrderService orderService, TimeSlotService timeSlotService, UserService userService) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.timeSlotService = timeSlotService;
        this.userService = userService;
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(Model model) {
        List<CustomerOrder> orders = orderService.getAllOrders();
        long pendingCount = orders.stream()
                .filter(order -> "Pending".equals(order.getOrderStatus()))
                .count();
        long readyCount = orders.stream()
                .filter(order -> "Ready".equals(order.getOrderStatus()))
                .count();
        long collectedCount = orders.stream()
                .filter(order -> "Collected".equals(order.getOrderStatus()))
                .count();

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("readyCount", readyCount);
        model.addAttribute("collectedCount", collectedCount);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Staff Dashboard | Campus Cafeteria");
        return "staff/dashboard";
    }

    @GetMapping("/staff/menu")
    public String staffMenu(Model model) {
        model.addAttribute("items", menuService.getAllItemsForStaff());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("activePage", "staffMenu");
        model.addAttribute("pageTitle", "Manage Menu | Campus Cafeteria");
        return "staff/menu-management";
    }

    @GetMapping("/staff/menu-management") // might not need this, keep for now.
    public String menuManagement(Model model) {
        List<MenuItem> items = menuService.getAllItemsForStaff();
        model.addAttribute("items", items);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("activePage", "staffMenu");
        model.addAttribute("pageTitle", "Menu Management | Campus Cafeteria");
        return "staff/menu-management";
    }

    @GetMapping("/staff/menu/new")
    public String addMenuItem(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Add Menu Item | Campus Cafeteria");
        return "staff/add-item";
    }

    @GetMapping("/staff/menu/{id}/edit")
    public String editMenuItem(@PathVariable Long id,
                               Model model) {
        menuService.getItemById(id).ifPresent(item -> model.addAttribute("item", item));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Edit Menu Item | Campus Cafeteria");
        return "staff/edit-item";
    }
    // POST /api/staff/categories
    // ─────────────────────────────────────────
    // DATABASE OPERATION: CREATE
    // Inserts a new row into the category table.
    //
    // Body: { "categoryName": "Dessert" }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> addCategory(
            @RequestBody Map<String, String> body) {

        String name = body.getOrDefault(
                "categoryName", "").trim();

        if (name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", "Category name is required."));
        }

        try {
            Category cat = categoryService.addCategory(name);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status",     "success",
                            "message",    "Category added.",
                            "categoryId", cat.getCategoryId(),
                            "categoryName", cat.getCategoryName()
                    ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }


    // ─────────────────────────────────────────
// DELETE /api/staff/categories/{id}
// ─────────────────────────────────────────
// DATABASE OPERATION: DELETE
// Removes a category from the categories table.
// Only safe if no menu items reference this category.
// ─────────────────────────────────────────
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(
            @PathVariable Long id) {

        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of(
                    "status",  "success",
                    "message", "Category deleted."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
// POST /api/staff/time-slots
// ─────────────────────────────────────────
// DATABASE OPERATION: CREATE
// Inserts a new time slot into time_slots table.
//
// Body: { "slotTime": "11:45", "maxOrders": 20 }
// ─────────────────────────────────────────
    @PostMapping("/time-slots")
    public ResponseEntity<Map<String, Object>> addTimeSlot(
            @RequestBody Map<String, Object> body) {

        String slotTime = body.getOrDefault(
                "slotTime", "").toString().trim();
        int maxOrders = body.containsKey("maxOrders")
                ? Integer.parseInt(
                body.get("maxOrders").toString())
                : 20;

        if (slotTime.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", "Slot time is required."));
        }

        try {
            TimeSlot slot = timeSlotService
                    .addTimeSlot(slotTime, maxOrders);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status",     "success",
                            "message",    "Time slot added.",
                            "timeSlotId", slot.getTimeSlotId(),
                            "slotTime",   slot.getSlotTime()
                    ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
// PATCH /api/staff/time-slots/{id}/toggle
// ─────────────────────────────────────────
// DATABASE OPERATION: UPDATE
// Toggles is_active on a time slot.
// Deactivated slots won't appear in order form.
// ─────────────────────────────────────────
    @PatchMapping("/time-slots/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleTimeSlot(
            @PathVariable Long id) {

        try {
            TimeSlot slot = timeSlotService.toggleSlot(id);
            return ResponseEntity.ok(Map.of(
                    "status",   "success",
                    "message",  "Slot " + (slot.getIsActive()
                            ? "activated." : "deactivated."),
                    "isActive", slot.getIsActive()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
// DELETE /api/staff/time-slots/{id}
// ─────────────────────────────────────────
// DATABASE OPERATION: DELETE
// ─────────────────────────────────────────
    @DeleteMapping("/time-slots/{id}")
    public ResponseEntity<Map<String, Object>> deleteTimeSlot(
            @PathVariable Long id) {

        try {
            timeSlotService.deleteSlot(id);
            return ResponseEntity.ok(Map.of(
                    "status",  "success",
                    "message", "Time slot deleted."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }


    // ─────────────────────────────────────────
// GET /api/staff/users
// ─────────────────────────────────────────
// DATABASE OPERATION: READ
// Returns all users. Admin only.
// ─────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers());
    }

    // ─────────────────────────────────────────
// POST /api/staff/users
// ─────────────────────────────────────────
// DATABASE OPERATION: CREATE
// Admin creates a staff or student account.
// Used to create staff accounts since
// public registration only creates students.
//
// Body: {
//   "username":  "newstaff",
//   "password":  "password123",
//   "firstName": "Jane",
//   "lastName":  "Smith",
//   "email":     "jane@cput.ac.za",
//   "role":      "STAFF"
// }
// ─────────────────────────────────────────
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, String> body) {

        String username  = body.getOrDefault(
                "username", "").trim();
        String password  = body.getOrDefault(
                "password", "");
        String firstName = body.getOrDefault(
                "firstName", "").trim();
        String lastName  = body.getOrDefault(
                "lastName", "").trim();
        String email     = body.getOrDefault(
                "email", "").trim();
        String role      = body.getOrDefault(
                "role", "STUDENT").trim().toUpperCase();

        // Validate role
        if (!List.of("STUDENT","STAFF","ADMIN")
                .contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message",
                            "Role must be STUDENT, STAFF, or ADMIN."));
        }

        try {
            userService.registerUser(
                    username, password,
                    firstName, lastName,
                    email, role);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                            "status",   "success",
                            "message",  "User created.",
                            "username", username,
                            "role",     role
                    ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
// PATCH /api/staff/users/{id}/role
// ─────────────────────────────────────────
// DATABASE OPERATION: UPDATE
// Changes a user's role. Admin only.
//
// Body: { "role": "STAFF" }
// ─────────────────────────────────────────
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String role = body.getOrDefault("role", "")
                .trim().toUpperCase();

        if (!List.of("STUDENT","STAFF","ADMIN")
                .contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message",
                            "Invalid role."));
        }

        try {
            userService.updateUserRole(id, role);
            return ResponseEntity.ok(Map.of(
                    "status",  "success",
                    "message", "Role updated to " + role + "."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message", e.getMessage()));
        }
    }

    //-------------------

    @PostMapping("/api/staff/menu")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addMenuItem(
            @RequestBody MenuItem item) {

        try {
            MenuItem savedItem = menuService.addItem(item);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Menu item added successfully.");
            response.put("itemName", savedItem.getItemName());
            response.put("itemId", savedItem.getMenuItemId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }
}
