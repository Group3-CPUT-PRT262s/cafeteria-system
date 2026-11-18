package com.group3.cafeteria_system.controller;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group3.cafeteria_system.model.CustomerOrder;
import com.group3.cafeteria_system.model.MenuItem;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.model.OrderItem;
import com.group3.cafeteria_system.service.CategoryService;
import com.group3.cafeteria_system.service.MenuService;
import com.group3.cafeteria_system.service.OrderService;
import com.group3.cafeteria_system.service.UserService;
import com.group3.cafeteria_system.service.WalletService;
import com.group3.cafeteria_system.service.CheckoutService;
import com.group3.cafeteria_system.service.ImageStorageService;
import com.group3.cafeteria_system.service.TimeSlotService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final MenuService menuService;
    private final OrderService orderService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TimeSlotService timeSlotService;
    private final CheckoutService checkoutService;
    private final WalletService walletService;
    private final ImageStorageService imageStorageService;

    public CustomerController(MenuService menuService,
                              OrderService orderService,
                              UserService userService,
                              CategoryService categoryService,
                              TimeSlotService timeSlotService,
                              CheckoutService checkoutService,
                              WalletService walletService,
                              ImageStorageService imageStorageService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.timeSlotService = timeSlotService;
        this.checkoutService = checkoutService;
        this.walletService = walletService;
        this.imageStorageService = imageStorageService;
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
        model.addAttribute("timeSlots", timeSlotService.getAvailableTimeSlots());
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        model.addAttribute("activePage", "menu");
        model.addAttribute("pageTitle", "Menu | Campus Cafeteria");

        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
        }

        return "customer/menu";
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
        model.addAttribute("timeSlots", timeSlotService.getAvailableTimeSlots());
        model.addAttribute("walletBalance", walletService.getBalance(user.getUserId()));
        model.addAttribute("activePage", "cart");
        model.addAttribute("pageTitle", "Checkout | Campus Cafeteria");
        return "customer/confirmation";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam Long timeSlotId,
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
                    user.getUserId(), timeSlotId, cart);
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
        List<CustomerOrder> orders = user.isStaff()
                ? orderService.getAllOrdersOldestFirst().stream()
                        .sorted(Comparator.comparing(CustomerOrder::getOrderedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .toList()
                : orderService.getOrdersByUser(user.getUserId()).stream()
                        .limit(5)
                        .toList();
        model.addAttribute("orders", orders);
        model.addAttribute("walletBalance", walletService.getBalance(user.getUserId()));
        model.addAttribute("walletTopUpAmounts", WalletService.DEMO_TOP_UP_AMOUNTS);
        model.addAttribute("walletTransactions", walletService.getTransactions(user.getUserId()));
        model.addAttribute("staffProfile", user.isStaff());
        model.addAttribute("orderHistoryUrl",
                user.isStaff() ? "/staff/orders" : "/customer/history");
        model.addAttribute("pageTitle", "My Profile | Campus Cafeteria");
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                @RequestParam(required = false) MultipartFile profilePicture,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        String newPictureUrl = null;
        try {
            if (profilePicture != null && !profilePicture.isEmpty()) {
                newPictureUrl = imageStorageService.storeProfilePicture(profilePicture);
            }
            String previousPictureUrl = user.getProfilePictureUrl();
            userService.updateProfile(user.getUsername(), firstName, lastName, email, newPictureUrl);
            if (newPictureUrl != null) {
                deleteUploadedImage(previousPictureUrl);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        } catch (RuntimeException | IOException e) {
            deleteUploadedImage(newPictureUrl);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    private void deleteUploadedImage(String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        try {
            imageStorageService.delete(imageUrl);
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Could not remove uploaded profile image {}: {}",
                    imageUrl, exception.getMessage());
        }
    }

    @GetMapping("/wallet")
    public String wallet(Authentication authentication, Model model,
                         @RequestParam(required = false) String success) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("walletBalance", walletService.getBalance(user.getUserId()));
        model.addAttribute("walletTopUpAmounts", WalletService.DEMO_TOP_UP_AMOUNTS);
        model.addAttribute("walletTransactions", walletService.getTransactions(user.getUserId()));
        model.addAttribute("success", success);
        model.addAttribute("activePage", "profile");
        model.addAttribute("pageTitle", "Campus Wallet | Campus Cafeteria");
        return "customer/wallet";
    }

    @PostMapping("/wallet/load/review")
    public String reviewWalletLoad(@RequestParam double amount,
                                   @RequestParam String paymentMethod,
                                   Authentication authentication,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        if (!WalletService.DEMO_TOP_UP_AMOUNTS.contains(amount)
                || !List.of("CARD", "BANK").contains(paymentMethod)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Choose a valid amount and payment method.");
            return "redirect:/wallet";
        }
        session.setAttribute("walletLoadUserId", user.getUserId());
        session.setAttribute("walletLoadAmount", amount);
        session.setAttribute("walletLoadMethod", paymentMethod);
        session.setAttribute("walletLoadKey", java.util.UUID.randomUUID().toString());
        model.addAttribute("amount", amount);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("pageTitle", "Verify wallet load | Campus Cafeteria");
        return "customer/wallet-review";
    }

    @PostMapping("/wallet/load/verify")
    public String verifyWalletLoad(Authentication authentication,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        Object pendingUserId = session.getAttribute("walletLoadUserId");
        Object amount = session.getAttribute("walletLoadAmount");
        Object method = session.getAttribute("walletLoadMethod");
        Object key = session.getAttribute("walletLoadKey");
        if (!user.getUserId().equals(pendingUserId)
                || !(amount instanceof Number)
                || !(method instanceof String)
                || !(key instanceof String)) {
            clearWalletLoad(session);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No pending wallet payment was found. Please start again.");
            return "redirect:/wallet";
        }
        try {
            double balance = walletService.loadCredits(user.getUserId(),
                    ((Number) amount).doubleValue(), (String) method, (String) key);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Simulated payment approved. Your wallet balance is R"
                            + String.format("%.2f", balance) + ".");
            clearWalletLoad(session);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/wallet";
    }

    @PostMapping("/wallet/load/cancel")
    public String cancelWalletLoad(Authentication authentication, HttpSession session) {
        User user = currentUser(authentication);
        Object pendingUserId = session.getAttribute("walletLoadUserId");
        Object amount = session.getAttribute("walletLoadAmount");
        Object method = session.getAttribute("walletLoadMethod");
        Object key = session.getAttribute("walletLoadKey");
        if (user != null && pendingUserId instanceof Long userId
                && user.getUserId().equals(userId) && amount instanceof Number number
                && method instanceof String paymentMethod && key instanceof String idempotencyKey) {
            walletService.recordTopUpOutcome(userId, number.doubleValue(), paymentMethod,
                    idempotencyKey, "CANCELLED");
        }
        clearWalletLoad(session);
        return "redirect:/wallet";
    }

    @PostMapping("/wallet/load/fail")
    public String failWalletLoad(Authentication authentication, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Object pendingUserId = session.getAttribute("walletLoadUserId");
        Object amount = session.getAttribute("walletLoadAmount");
        Object method = session.getAttribute("walletLoadMethod");
        Object key = session.getAttribute("walletLoadKey");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getUserId().equals(pendingUserId)
                || !(amount instanceof Number)
                || !(method instanceof String)
                || !(key instanceof String)) {
            clearWalletLoad(session);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No pending wallet payment was found. Please start again.");
            return "redirect:/wallet";
        }
        walletService.recordTopUpOutcome(user.getUserId(), ((Number) amount).doubleValue(),
                (String) method, (String) key, "FAILED");
        clearWalletLoad(session);
        redirectAttributes.addFlashAttribute("errorMessage",
                "The simulated bank declined the payment. Your wallet was not credited.");
        return "redirect:/wallet";
    }

    private void clearWalletLoad(HttpSession session) {
        session.removeAttribute("walletLoadUserId");
        session.removeAttribute("walletLoadAmount");
        session.removeAttribute("walletLoadMethod");
        session.removeAttribute("walletLoadKey");
    }

    @PostMapping("/profile/delete-account")
    public String deleteAccount(Authentication authentication,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            userService.deleteAccount(user.getUserId());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/profile";
        }

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        return "redirect:/login?deleted=true";
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
