package com.group3.cafeteria_system.controller;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.model.OrderItem;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import com.group3.cafeteria_system.service.OrderService;
import com.group3.cafeteria_system.service.UserService;
import com.group3.cafeteria_system.service.CheckoutService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerController {

    private final MenuService menuService;
    private final OrderService orderService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TimeSlotRepository timeSlotRepository;
    private final CheckoutService checkoutService;

    public CustomerController(MenuService menuService,
                              OrderService orderService,
                              UserService userService,
                              CategoryService categoryService,
                              TimeSlotRepository timeSlotRepository,
                              CheckoutService checkoutService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.timeSlotRepository = timeSlotRepository;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/customer/menu")
    public String showMenu(Model model,
                           Authentication authentication,
                           @RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "success", required = false) String success) {
        List<MenuItem> items = menuService.getAllActiveItems();
        model.addAttribute("items", items);
        model.addAttribute("menuItems", items);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("timeSlots", timeSlotRepository.findByIsActiveTrue());
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        model.addAttribute("activePage", "menu");
        model.addAttribute("pageTitle", "Menu | Campus Cafeteria");

        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }

        return "customer/menu";
    }

    @PostMapping("/customer/order")
    public String placeOrder(@RequestParam("timeSlotId") Long timeSlotId,
                             @RequestParam Map<String, String> params,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> optionalUser = userService.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();
        Map<Long, Integer> itemQuantities = new HashMap<>();
        for (MenuItem menuItem : menuService.getAllActiveItems()) {
            String quantityParam = params.get("quantity_" + menuItem.getMenuItemId());
            if (quantityParam == null || quantityParam.isBlank()) {
                continue;
            }

            try {
                int quantity = Integer.parseInt(quantityParam);
                if (quantity > 0) {
                    itemQuantities.put(menuItem.getMenuItemId(), quantity);
                }
            } catch (NumberFormatException ignored) {
                // Skip invalid quantity values
            }
        }

        if (itemQuantities.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one item before placing an order.");
            return "redirect:/customer/menu";
        }

        try {
            CustomerOrder order = orderService.placeOrder(user.getUserId(), timeSlotId, itemQuantities);
            redirectAttributes.addFlashAttribute("success", "Order " + order.getOrderId() + " placed successfully.");
            return "redirect:/customer/history";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/menu";
        }
    }

    @GetMapping("/customer/history")
    public String orderHistory(Model model,
                               Authentication authentication,
                               @RequestParam(value = "success", required = false) String success) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> optionalUser = userService.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();
        List<CustomerOrder> orders = orderService.getOrdersByUser(user.getUserId());
        model.addAttribute("orders", orders);
        model.addAttribute("username", username);
        model.addAttribute("success", success);
        return "customer/history";
    }

    @GetMapping("/confirmation")
    public String confirmation(HttpSession session, Authentication authentication,
                               Model model, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your cart is empty. Add an item before checking out.");
            return "redirect:/menu";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("timeSlots", timeSlotRepository.findByIsActiveTrue());
        model.addAttribute("activePage", "cart");
        model.addAttribute("pageTitle", "Checkout | Campus Cafeteria");
        return "customer/confirmation";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam Long timeSlotId,
                           @RequestParam String paymentMethod,
                           HttpSession session,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        Map<Long, Integer> cart = getCart(session);
        try {
            CustomerOrder order = checkoutService.checkout(
                    user.getUserId(), timeSlotId, cart, paymentMethod);
            session.removeAttribute("cart");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderId() + " has been placed.");
            return "redirect:/orders/" + order.getOrderId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/confirmation";
        }
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@org.springframework.web.bind.annotation.PathVariable Long id,
                              Authentication authentication, Model model) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        CustomerOrder order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found."));
        if (!order.getUserId().equals(user.getUserId()) && !user.isStaff()) {
            return "redirect:/customer/history";
        }

        List<Map<String, Object>> orderLines = new ArrayList<>();
        for (OrderItem item : orderService.getItemsByOrderId(id)) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("itemName", menuService.getItemById(item.getMenuItemId())
                    .map(MenuItem::getItemName).orElse("Menu item"));
            line.put("quantity", item.getQuantity());
            line.put("subtotal", item.getQuantity() * item.getUnitPrice());
            orderLines.add(line);
        }
        model.addAttribute("order", order);
        model.addAttribute("orderLines", orderLines);
        model.addAttribute("pageTitle", "Order #" + id + " | Campus Cafeteria");
        return "customer/order-detail";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "My Profile | Campus Cafeteria");
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            userService.updateProfile(user.getUsername(), firstName, lastName, email);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.findByUsername(authentication.getName()).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        return cart == null ? Map.of() : new HashMap<>(cart);
    }
}
