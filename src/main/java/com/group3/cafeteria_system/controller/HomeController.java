package com.group3.cafeteria_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF")
                        || auth.getAuthority().equals("ROLE_ADMIN"));

        if (isStaff) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/customer/menu";
    }
}