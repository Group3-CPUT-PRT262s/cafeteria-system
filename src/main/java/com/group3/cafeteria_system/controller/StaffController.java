package com.group3.cafeteria_system.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import com.group3.cafeteria_system.service.OrderService;

@Controller
public class StaffController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    public StaffController(MenuService menuService,
                           CategoryService categoryService,
                           OrderService orderService) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.orderService = orderService;
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

    @GetMapping("/staff/menu-management")
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
}
