package com.group3.cafeteria_system;

import com.group3.cafeteria_system.model.*;
import com.group3.cafeteria_system.repository.*;
import com.group3.cafeteria_system.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitialiser implements CommandLineRunner {

    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final InventoryService inventoryService;

    public DataInitialiser(UserService userService, CategoryRepository categoryRepository, MenuItemRepository menuItemRepository, TimeSlotRepository timeSlotRepository, InventoryService inventoryService) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) throws Exception {

        // mock test users
        if (!userService.userExists("student1")) {
            userService.createUser("student1", "test123", "STUDENT");
        }
        if (!userService.userExists("lecturer1")) {
            userService.createUser("lecturer1", "test123", "STUDENT");
        }
        if (!userService.userExists("staff1")) {
            userService.createUser("staff1", "staff123", "STAFF");
        }
        if (!userService.userExists("admin1")) {
            userService.createUser("admin1", "admin123", "ADMIN");
        }

        // Seed time slots
        if (timeSlotRepository.count() == 0) {
            String[] slots = {
                    "12:00", "12:15", "12:30", "12:45",
                    "13:00", "13:15", "13:30"
            };
            for (String time : slots) {
                timeSlotRepository.save(new TimeSlot(time, 20));
            }
        }

        // Mock categories
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("Hot Meal"));
            categoryRepository.save(new Category("Snack"));
            categoryRepository.save(new Category("Beverage"));
        }

        // Mock menu items + inventory
        if (menuItemRepository.count() == 0) {
            Long hotMealId = categoryRepository
                    .findByCategoryName("Hot Meal").get().getCategoryId();
            Long snackId = categoryRepository
                    .findByCategoryName("Snack").get().getCategoryId();
            Long beverageId = categoryRepository
                    .findByCategoryName("Beverage").get().getCategoryId();

            MenuItem chickenWrap = menuItemRepository.save(new MenuItem(
                    hotMealId, "Chicken Wrap",
                    "Grilled chicken with salad and sauce",
                    45.00, "Available"));

            MenuItem veggieBurger = menuItemRepository.save(new MenuItem(
                    hotMealId, "Veggie Burger",
                    "Plant-based patty with chips",
                    40.00, "Available"));

            MenuItem cheeseToastie = menuItemRepository.save(new MenuItem(
                    snackId, "Cheese Toastie",
                    "Toasted cheese sandwich",
                    25.00, "Low Stock"));

            MenuItem fruitSalad = menuItemRepository.save(new MenuItem(
                    snackId, "Fruit Salad",
                    "Mixed seasonal fruit",
                    20.00, "Available"));

            MenuItem coke = menuItemRepository.save(new MenuItem(
                    beverageId, "Coke 330ml",
                    "Cold fizzy drink",
                    18.00, "Available"));

            MenuItem stillWater = menuItemRepository.save(new MenuItem(
                    beverageId, "Still Water",
                    "500ml bottle",
                    12.00, "Sold Out"));

            // Mock inventory records
            // Quantities chosen to match the seeded status above
            inventoryService.createInventory(
                    chickenWrap.getMenuItemId(), 30, 5, "portions");
            inventoryService.createInventory(
                    veggieBurger.getMenuItemId(), 25, 5, "portions");
            inventoryService.createInventory(
                    cheeseToastie.getMenuItemId(), 4, 5, "portions");
            inventoryService.createInventory(
                    fruitSalad.getMenuItemId(), 20, 5, "portions");
            inventoryService.createInventory(
                    coke.getMenuItemId(), 40, 10, "units");
            inventoryService.createInventory(
                    stillWater.getMenuItemId(), 0, 10, "units");
        }

        System.out.println("--- Data initialisation complete ---");
        System.out.println("Test users: student1/test123, " +
                "staff1/staff123, admin1/admin123");
    }
}