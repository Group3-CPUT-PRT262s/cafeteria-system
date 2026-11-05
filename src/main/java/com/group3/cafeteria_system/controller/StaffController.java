package com.group3.cafeteria_system.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.time.LocalDate;
import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.group3.cafeteria_system.service.ImageStorageService;

@Controller
public class StaffController {

    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final TimeSlotService timeSlotService;
    private final UserService userService;
    private final ImageStorageService imageStorageService;

    public StaffController(MenuService menuService, CategoryService categoryService,
                           OrderService orderService, TimeSlotService timeSlotService,
                           UserService userService, ImageStorageService imageStorageService) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.timeSlotService = timeSlotService;
        this.userService = userService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Staff Dashboard | Campus Cafeteria");
        return "staff/dashboard";
    }

    @GetMapping("/staff/orders")
    public String staffOrders(Model model) {
        model.addAttribute("activePage", "staffOrders");
        model.addAttribute("pageTitle", "Orders | Campus Cafeteria");
        return "staff/orders";
    }

    @GetMapping("/api/staff/dashboard/stats")
    @ResponseBody
    public Map<String, Object> dashboardStats() {
        List<CustomerOrder> orders = orderService.getAllOrders();
        long pending = countStatus(orders, "Pending", "Confirmed");
        long prepared = countStatus(orders, "Prepared", "Preparing");
        long ready = countStatus(orders, "Ready");
        long collected = countStatus(orders, "Collected");
        long today = orders.stream()
                .filter(order -> order.getOrderedAt() != null
                        && order.getOrderedAt().toLocalDate().equals(LocalDate.now()))
                .filter(order -> !"Cancelled".equals(order.getOrderStatus()))
                .count();

        double collectedRevenue = orders.stream()
                .filter(order -> "Collected".equals(order.getOrderStatus()))
                .mapToDouble(order -> order.getTotalAmount() == null ? 0 : order.getTotalAmount())
                .sum();

        List<MenuItem> menuItems = menuService.getAllItemsForStaff();
        long activeMenuItems = menuItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .count();
        long availableMenuItems = menuItems.stream()
                .filter(MenuItem::isAvailable)
                .count();

        List<Map<String, Object>> recentOrders = orders.stream()
                .sorted(Comparator.comparing(CustomerOrder::getOrderedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(this::toRecentOrder)
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", (long) orders.size());
        stats.put("today", today);
        stats.put("pending", pending);
        stats.put("prepared", prepared);
        stats.put("ready", ready);
        stats.put("collected", collected);
        stats.put("collectedRevenue", collectedRevenue);
        stats.put("averageCollectedOrder", collected == 0 ? 0 : collectedRevenue / collected);
        stats.put("activeMenuItems", activeMenuItems);
        stats.put("availableMenuItems", availableMenuItems);
        stats.put("recentOrders", recentOrders);
        return stats;
    }

    private Map<String, Object> toRecentOrder(CustomerOrder order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("customerName", userService.findById(order.getUserId())
                .map(User::getDisplayName).orElse("Customer"));
        result.put("orderedAt", order.getOrderedAt());
        result.put("totalAmount", order.getTotalAmount());
        result.put("orderStatus", order.getOrderStatus());
        return result;
    }

    @GetMapping("/api/staff/orders")
    @ResponseBody
    public List<Map<String, Object>> staffOrderData() {
        return orderService.getAllOrdersOldestFirst().stream()
                .map(this::toStaffOrder)
                .toList();
    }

    @PatchMapping("/api/staff/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        status = status == null ? "" : status.trim();
        if (!List.of("Prepared", "Ready", "Collected", "Cancelled").contains(status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Choose a valid order status."));
        }

        try {
            CustomerOrder order = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "orderStatus", order.getOrderStatus(),
                    "message", "Order #" + id + " marked " + status + "."));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }

    @PatchMapping("/api/staff/orders/{orderId}/items/{menuItemId}/out-of-stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markOrderItemOutOfStock(
            @PathVariable Long orderId,
            @PathVariable Long menuItemId) {
        try {
            MenuItem item = orderService.markOrderItemOutOfStock(orderId, menuItemId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", item.getItemName() + " marked out of stock. The order was not cancelled."));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }

    private long countStatus(List<CustomerOrder> orders, String... statuses) {
        return orders.stream()
                .filter(order -> List.of(statuses).contains(order.getOrderStatus()))
                .count();
    }

    private Map<String, Object> toStaffOrder(CustomerOrder order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("customerName", userService.findById(order.getUserId())
                .map(User::getDisplayName).orElse("Customer"));
        result.put("orderedAt", order.getOrderedAt());
        result.put("totalAmount", order.getTotalAmount());
        result.put("orderStatus", order.getOrderStatus());
        result.put("timeSlot", timeSlotService.findById(order.getTimeSlotId())
                .map(TimeSlot::getSlotTime).orElse("Unavailable"));
        result.put("items", orderService.getItemsByOrderId(order.getOrderId()).stream()
                .map(orderItem -> {
                    MenuItem menuItem = menuService.getItemById(orderItem.getMenuItemId()).orElse(null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("menuItemId", orderItem.getMenuItemId());
                    item.put("itemName", menuItem == null ? "Menu item" : menuItem.getItemName());
                    item.put("quantity", orderItem.getQuantity());
                    item.put("unitPrice", orderItem.getUnitPrice());
                    item.put("subtotal", orderItem.getSubtotal());
                    item.put("available", menuItem != null && menuItem.isAvailable());
                    item.put("itemStatus", menuItem == null ? "Unavailable" : menuItem.getStatus());
                    return item;
                })
                .toList());
        return result;
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
    public String editMenuItem(@PathVariable Long id, Model model) {
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

    @PostMapping("/api/staff/categories")
    public ResponseEntity<Map<String, Object>> addCategory(
            @RequestBody Map<String, String> body) {

        String name = body == null || body.get("categoryName") == null
                ? "" : body.get("categoryName").trim();

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
    @PutMapping("/api/staff/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Category category = categoryService.updateCategory(
                    id, body == null || body.get("categoryName") == null
                            ? "" : body.get("categoryName"));
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Category updated.",
                    "categoryId", category.getCategoryId(),
                    "categoryName", category.getCategoryName()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }

    @DeleteMapping("/api/staff/categories/{id}")
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
    @PostMapping("/api/staff/time-slots")
    public ResponseEntity<Map<String, Object>> addTimeSlot(
            @RequestBody Map<String, Object> body) {

        Object requestedTime = body == null ? null : body.get("slotTime");
        String slotTime = requestedTime == null ? "" : requestedTime.toString().trim();
        Object capacity = body == null ? null : body.get("maxOrders");
        int maxOrders;
        try {
            maxOrders = capacity == null ? 20 : Integer.parseInt(capacity.toString());
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Max orders must be a whole number."));
        }

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
    @PatchMapping("/api/staff/time-slots/{id}/toggle")
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
    @PutMapping("/api/staff/time-slots/{id}")
    public ResponseEntity<Map<String, Object>> updateTimeSlot(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Object requestedTime = body == null ? null : body.get("slotTime");
        String slotTime = requestedTime == null ? "" : requestedTime.toString().trim();
        Object capacity = body == null ? null : body.get("maxOrders");
        int maxOrders;
        try {
            maxOrders = Integer.parseInt(capacity == null ? "" : capacity.toString());
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Max orders must be a whole number."));
        }
        try {
            TimeSlot slot = timeSlotService.updateTimeSlot(id, slotTime, maxOrders);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Time slot updated.",
                    "timeSlotId", slot.getTimeSlotId(),
                    "slotTime", slot.getSlotTime(),
                    "maxOrders", slot.getMaxOrders()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }

    @DeleteMapping("/api/staff/time-slots/{id}")
    public ResponseEntity<Map<String, Object>> deleteTimeSlot(
            @PathVariable Long id) {

        try {
            timeSlotService.deleteSlot(id);
            return ResponseEntity.ok(Map.of(
                    "status",  "success",
                    "message", "Time slot deleted."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
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
    @GetMapping("/api/staff/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream()
                .map(this::toAdminUser)
                .toList());
    }

    private Map<String, Object> toAdminUser(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("firstName", user.getFirstName());
        result.put("lastName", user.getLastName());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("accountStatus", user.getAccountStatus());
        result.put("createdAt", user.getCreatedAt());
        return result;
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
    @PostMapping("/api/staff/users")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, String> body) {

        Map<String, String> fields = body == null ? Map.of() : body;
        String username  = fields.get("username") == null
                ? "" : fields.get("username").trim();
        String password  = fields.getOrDefault(
                "password", "");
        String firstName = fields.get("firstName") == null
                ? "" : fields.get("firstName").trim();
        String lastName  = fields.get("lastName") == null
                ? "" : fields.get("lastName").trim();
        String email     = fields.get("email") == null
                ? "" : fields.get("email").trim();
        String role      = fields.get("role") == null
                ? "STUDENT" : fields.get("role").trim().toUpperCase();

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
    @PatchMapping("/api/staff/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String role = body == null || body.get("role") == null
                ? "" : body.get("role").trim().toUpperCase();

        if (!List.of("STUDENT","STAFF","ADMIN")
                .contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status",  "error",
                            "message",
                            "Invalid role."));
        }

        try {
            userService.updateUserRole(
                    userService.getUserIdByUsername(authentication.getName()), id, role);
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

    @PatchMapping("/api/staff/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String status = body == null || body.get("accountStatus") == null
                ? "" : body.get("accountStatus").trim().toUpperCase();
        try {
            userService.updateAccountStatus(
                    userService.getUserIdByUsername(authentication.getName()), id, status);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Account status updated to " + status + "."));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }

    //-------------------

    @PostMapping(value = "/api/staff/menu", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/api/staff/menu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addMenuItemWithImage(
            @RequestParam String itemName,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam Long categoryId,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile image) {
        String imageUrl = null;
        try {
            MenuItem item = new MenuItem();
            item.setItemName(itemName);
            item.setDescription(description);
            item.setPrice(price);
            item.setCategoryId(categoryId);
            item.setStatus(status);
            if (image != null && !image.isEmpty()) {
                imageUrl = imageStorageService.storeMenuItemImage(image);
                item.setImageUrl(imageUrl);
            }

            MenuItem savedItem = menuService.addItem(item);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "Menu item added successfully.",
                    "itemName", savedItem.getItemName(),
                    "itemId", savedItem.getMenuItemId()));
        } catch (IOException | RuntimeException exception) {
            deleteUploadedImage(imageUrl);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage() == null
                            ? "Unable to save the menu item." : exception.getMessage()));
        }
    }


    /* PATCH /api/staff/menu/{id}/remove

     DATABASE OPERATION: UPDATE
     Soft-deletes a menu item by setting is_active = false.
     The database record is preserved.
    */
    @PatchMapping("/api/staff/menu/{id}/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeMenuItem(
            @PathVariable Long id) {

        try {
            menuService.removeItem(id);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Menu item deactivated.",
                    "isActive", false
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }


    /*
     PATCH /api/staff/menu/{id}/activate
     DATABASE OPERATION: UPDATE
     Restores a menu item by setting is_active = true.
    */
    @PatchMapping("/api/staff/menu/{id}/activate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> activateMenuItem(
            @PathVariable Long id) {

        try {
            menuService.restoreItem(id);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Menu item activated.",
                    "isActive", true
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────
// PUT /api/staff/menu/{id}
// ─────────────────────────────────────────
// DATABASE OPERATION: UPDATE
// Updates the details of an existing menu item.
// ─────────────────────────────────────────
    @PutMapping("/api/staff/menu/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateMenuItem(
            @PathVariable Long id,
            @RequestBody MenuItem updatedItem) {

        try {
            MenuItem savedItem = menuService.updateItem(id, updatedItem);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Menu item updated successfully.",
                    "itemId", savedItem.getMenuItemId(),
                    "itemName", savedItem.getItemName()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/api/staff/menu/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateMenuItemWithImage(
            @PathVariable Long id,
            @RequestParam String itemName,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam Long categoryId,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile image) {
        String imageUrl = null;
        String previousImageUrl = null;
        try {
            MenuItem existingItem = menuService.getItemById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + id));
            previousImageUrl = existingItem.getImageUrl();

            MenuItem updatedItem = new MenuItem();
            updatedItem.setItemName(itemName);
            updatedItem.setDescription(description);
            updatedItem.setPrice(price);
            updatedItem.setCategoryId(categoryId);
            updatedItem.setStatus(status);
            if (image != null && !image.isEmpty()) {
                imageUrl = imageStorageService.storeMenuItemImage(image);
                updatedItem.setImageUrl(imageUrl);
            }

            MenuItem savedItem = menuService.updateItem(id, updatedItem);
            if (imageUrl != null) {
                deleteUploadedImage(previousImageUrl);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Menu item updated successfully.",
                    "itemId", savedItem.getMenuItemId(),
                    "itemName", savedItem.getItemName()));
        } catch (IOException | RuntimeException exception) {
            deleteUploadedImage(imageUrl);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage() == null
                            ? "Unable to update the menu item." : exception.getMessage()));
        }
    }

    private void deleteUploadedImage(String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        try {
            imageStorageService.delete(imageUrl);
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Could not remove uploaded image {}: {}", imageUrl, exception.getMessage());
        }
    }

    @PatchMapping("/api/staff/menu/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateMenuItemStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || !List.of("Available", "Low Stock", "Sold Out").contains(status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Choose Available, Low Stock, or Sold Out."));
        }
        try {
            MenuItem item = menuService.updateStatus(id, status);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", item.getItemName() + " availability updated.",
                    "itemStatus", item.getStatus()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()));
        }
    }
}
