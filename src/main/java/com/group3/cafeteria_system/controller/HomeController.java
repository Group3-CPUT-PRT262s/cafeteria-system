package com.group3.cafeteria_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        return isStaff(authentication)
                ? "redirect:/staff/dashboard"
                : "redirect:/customer/menu";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return isStaff(authentication)
                ? "redirect:/staff/dashboard"
                : "redirect:/customer/menu";
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF")
                        || auth.getAuthority().equals("ROLE_ADMIN"));
    }
}