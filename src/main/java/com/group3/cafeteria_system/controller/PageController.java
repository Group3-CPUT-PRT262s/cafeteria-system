package com.group3.cafeteria_system.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.model.MenuItem;
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
                          UserService userService) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.timeSlotRepository = timeSlotRepository;
        this.userService = userService;
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