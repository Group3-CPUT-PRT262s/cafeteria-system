package com.group3.cafeteria_system;

import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.TimeSlot;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import com.group3.cafeteria_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    // CommandLineRunner means this runs automatically
    // every time the application starts.

    @Autowired
    private UserService userService;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Override
    public void run(String... args) throws Exception {

        // ── Seed test users ───────────────────────
        // Only creates users if they don't already exist
        // so re-running the app doesn't create duplicates

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

        // ── Seed time slots ───────────────────────
        if (timeSlotRepository.count() == 0) {
            String[] slots = {
                    "12:00", "12:15", "12:30", "12:45",
                    "13:00", "13:15", "13:30"
            };
            for (String time : slots) {
                TimeSlot slot = new TimeSlot();
                slot.setSlotTime(time);
                slot.setIsActive(true);
                timeSlotRepository.save(slot);
            }
        }

        // ── Seed menu items ───────────────────────
        // These are temp values for now... just testing the routes and DB
        if (menuItemRepository.count() == 0) {
            menuItemRepository.save(new MenuItem(
                    "Chicken Wrap",
                    "Grilled chicken with salad and sauce",
                    45.00, "Hot Meal", "Available"));

            menuItemRepository.save(new MenuItem(
                    "Veggie Burger",
                    "Plant-based patty with chips",
                    40.00, "Hot Meal", "Available"));

            menuItemRepository.save(new MenuItem(
                    "Cheese Toastie",
                    "Toasted cheese sandwich",
                    25.00, "Snack", "Low Stock"));

            menuItemRepository.save(new MenuItem(
                    "Fruit Salad",
                    "Mixed seasonal fruit",
                    20.00, "Snack", "Available"));

            menuItemRepository.save(new MenuItem(
                    "Coke 330ml",
                    "Cold fizzy drink",
                    18.00, "Beverage", "Available"));

            menuItemRepository.save(new MenuItem(
                    "Still Water",
                    "500ml bottle",
                    12.00, "Beverage", "Sold Out"));
        }

        System.out.println("--- Data initialisation complete ---");
        System.out.println("Test users: student1/test123, " +
                "staff1/staff123, admin1/admin123");
    }
}