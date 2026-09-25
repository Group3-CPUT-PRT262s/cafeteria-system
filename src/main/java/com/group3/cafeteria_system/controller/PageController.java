package com.group3.cafeteria_system.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.repository.MenuItemRepository;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import com.group3.cafeteria_system.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final TimeSlotRepository timeSlotRepository;
    private final UserService userService;

    public PageController(MenuService menuService,
                          CategoryService categoryService,
                          TimeSlotRepository timeSlotRepository,
                          UserService userService,
                          MenuItemRepository menuItemRepository) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.timeSlotRepository = timeSlotRepository;
        this.userService = userService;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/menu";
    }

    // ── Customer menu page ────────────────────
    // GET /menu
    // Renders the customer-facing menu page.
    // Supports optional category filter via ?categoryId=
    //
    // DATABASE OPERATION: READ
    // Retrieves all active menu items from the
    // menu_items table where is_active = true.
    // If categoryId is provided, filters by
    // category_id FK. Demonstrates SELECT query.
    @GetMapping("/menu")
    public String menuPage(
            @RequestParam(required = false) Long categoryId,
            HttpSession session,
            Model model) {

        // READ — fetch active menu items from DB
        List<MenuItem> items = (categoryId != null)
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllActiveItems();

        // READ — fetch all categories for filter bar
        List<Category> categories = categoryService.getAllCategories();

        // READ — cart count from session
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        int cartCount = cartItemCount(cart);

        model.addAttribute("items", items);
        model.addAttribute("categories", categories);
        model.addAttribute("activeCategoryId", categoryId);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("activePage", "menu");
        model.addAttribute("pageTitle", "Menu | Campus Cafeteria");

        return "customer/menu";
    }

    // ── Cart page ─────────────────────────────
    // GET /cart
    // Renders the cart review page.
    @GetMapping("/cart")
    public String cartPage(
            HttpSession session,
            Model model) {

        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart =
                (Map<Long, Integer>) session.getAttribute("cart");

        int cartCount = cartItemCount(cart);

        model.addAttribute("cartCount",  cartCount);
        model.addAttribute("activePage", "cart");
        model.addAttribute("pageTitle",
                "Your Cart | Campus Cafeteria");

        return "customer/cart";
    }

    // ── Order history page ────────────────────
    @GetMapping("/orders")
    public String ordersPage(Model model) {
        model.addAttribute("activePage", "orders");
        model.addAttribute("pageTitle", "My Orders | Campus Cafeteria");
        return "redirect:/customer/history";
    }

    // ── Order detail page ─────────────────────
    @GetMapping("/orders/{id}")
    public String orderDetailPage(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        model.addAttribute("orderId", id);
        model.addAttribute("activePage", "orders");
        model.addAttribute("pageTitle", "Order #" + id + " | Campus Cafeteria");
        return "customer/order-detail";
    }

    /* 
    @PostMapping("/payment")
    public String processConfirmation(@RequestParam(required = false) String pickupTime) {
        // TODO: 
        // Here you would normally save the pickupTime to the database
        // Then, navigate the user to the payment page
        System.out.println("Going to payment page");
        return "customer/payment"; 
    }
    */

    @PostMapping("/payment")
    public String processConfirmation(@RequestParam Long timeSlotId, HttpSession session) {
        // Save the user's chosen time slot into their session so we can access it during final checkout
        session.setAttribute("selectedTimeSlot", timeSlotId);

        // TODO: 
        // Here you would normally save the pickupTime to the database
        // Then, navigate the user to the payment page
        System.out.println("Going to payment page");
        
        // Route to the payment page
        return "customer/payment"; 
    }

    @PostMapping("/order-detail")
    public String processPayment(@RequestParam String paymentMethod, HttpSession session, Model model) {
        
        session.setAttribute("selectedPayment", paymentMethod);
        
        @SuppressWarnings("unchecked")
        Map<Long, Integer> sessionCart = (Map<Long, Integer>) session.getAttribute("cart");

        double calculatedTotal = 0.0;
        
        // Maps to pass the exact item details to the HTML
        List<Map<String, Object>> receiptItems = new ArrayList<>();

        if (sessionCart != null && !sessionCart.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : sessionCart.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();

                Optional<MenuItem> itemOpt = menuItemRepository.findById(itemId);
                
                if (itemOpt.isPresent()) {
                    MenuItem item = itemOpt.get();
                    
                    calculatedTotal += (item.getPrice() * quantity);

                    Map<String, Object> itemDetails = new HashMap<>();
                    itemDetails.put("itemName", item.getItemName());
                    itemDetails.put("quantity", quantity);
                    itemDetails.put("price", item.getPrice());
                    itemDetails.put("subtotal", item.getPrice() * quantity);
                    
                    receiptItems.add(itemDetails);
                }
            }
        }

     
        CustomerOrder currentOrder = new CustomerOrder();
        currentOrder.setOrderId(9999L); // Still a test ID integrated to the database save
        currentOrder.setTotalAmount(calculatedTotal);
        
        model.addAttribute("order", currentOrder);
        model.addAttribute("orderItems", receiptItems);
        
        session.removeAttribute("cart");

        return "customer/order-detail"; 
    }

    private MenuItemRepository menuItemRepository;

    @GetMapping("/confirmation")
    public String showConfirmationPage(HttpSession session, Model model) {
        
        @SuppressWarnings("unchecked")
        Map<Long, Integer> sessionCart = (Map<Long, Integer>) session.getAttribute("cart");

        // Use a Map instead of a DTO List
        Map<MenuItem, Integer> displayCart = new HashMap<>();
        double cartTotal = 0.0;

        if (sessionCart != null && !sessionCart.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : sessionCart.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();

                Optional<MenuItem> itemOpt = menuItemRepository.findById(java.util.Objects.requireNonNull(itemId));

                
                if (itemOpt.isPresent()) {
                    MenuItem item = itemOpt.get();
                    
                    // Put the full MenuItem and the quantity directly into the map
                    displayCart.put(item, quantity);
                    cartTotal += (item.getPrice() * quantity);
                }
            }
        }

        model.addAttribute("cartItems", displayCart);
        model.addAttribute("totalPrice", cartTotal);

        return "customer/confirmation"; 
    }

    

    private int cartItemCount(Map<Long, Integer> cart) {
        if (cart == null || cart.isEmpty()) {
            return 0;
        }
        return cart.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(value -> value)
                .sum();
    }


    @GetMapping("/staff/settings")
    public String staffSettings(Model model) {
        model.addAttribute("categories",
                categoryService.getAllCategories());
        model.addAttribute("timeSlots",
                timeSlotRepository.findAll());
        model.addAttribute("users",
                userService.getAllUsers());
        model.addAttribute("activePage",  "settings");
        model.addAttribute("pageTitle",
                "Settings | Campus Cafeteria");
        return "staff/settings";
    }
}