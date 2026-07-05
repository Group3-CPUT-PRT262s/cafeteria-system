package com.group3.cafeteria_system.controller;

import java.util.*;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group3.cafeteria_system.model.*;
import com.group3.cafeteria_system.repository.TimeSlotRepository;
import com.group3.cafeteria_system.service.*;

@Controller
public class CustomerController {

    private final MenuService menuService;
    private final OrderService orderService;
    private final UserService userService;
    private final TimeSlotRepository timeSlotRepository;

    public CustomerController(MenuService menuService,
                              OrderService orderService,
                              UserService userService,
                              TimeSlotRepository timeSlotRepository) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.userService = userService;
        this.timeSlotRepository = timeSlotRepository;
    }

    @GetMapping("/customer/menu")
    public String showMenu(Model model,
                           Authentication authentication,
                           @RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "success", required = false) String success) {
        List<MenuItem> menuItems = menuService.getAllActiveItems();
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("timeSlots", timeSlotRepository.findByIsActiveTrue());
        model.addAttribute("error", error);
        model.addAttribute("success", success);

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
        String string = "redirect:/login"; //change later
        if (authentication == null || !authentication.isAuthenticated()) {
            return string;
        }

        String username = authentication.getName();
        Optional<User> optionalUser = userService.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return string;
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

        try {
            CustomerOrder order = orderService.placeOrder(user.getUserId(), timeSlotId, itemQuantities);
            redirectAttributes.addAttribute("success", "Order " + order.getOrderId() + " placed successfully.");
            return "redirect:/customer/history";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
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
}

