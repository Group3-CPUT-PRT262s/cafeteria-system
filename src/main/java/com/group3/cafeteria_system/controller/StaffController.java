package com.group3.cafeteria_system.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("orders", orders);
        return "staff/dashboard";
    }

    @GetMapping("/staff/menu-management")
    public String menuManagement(Model model) {
        List<MenuItem> items = menuService.getAllItemsForStaff();
        model.addAttribute("items", items);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "staff/menu-management";
    }

    @GetMapping("/staff/edit-item")
    public String editItem(@RequestParam(value = "id", required = false) Long id,
                           Model model) {
        if (id != null) {
            menuService.getItemById(id).ifPresent(item -> model.addAttribute("item", item));
        }
        model.addAttribute("categories", categoryService.getAllCategories());
        return "staff/edit-item";
    }
}
