package com.group3.cafeteria_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                       @RequestParam(required = false) String logout,
                       @RequestParam(value = "invalid-session", required = false) String invalidSession,
                       Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        if (invalidSession != null) {
            model.addAttribute("error", "Your session has expired. Please login again.");
        }

        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return switch (role) {
            case "ROLE_STUDENT", "ROLE_STAFF" -> "redirect:/customer/menu";
            case "ROLE_ADMIN" -> "redirect:/staff/dashboard";
            default -> "redirect:/login";
        }; // Both students and staff see the menu
        // Admin goes to staff dashboard
    }

    @GetMapping("/logout")
    public String logout() {
        SecurityContextHolder.clearContext();
        return "redirect:/login?logout=true";
    }
}
