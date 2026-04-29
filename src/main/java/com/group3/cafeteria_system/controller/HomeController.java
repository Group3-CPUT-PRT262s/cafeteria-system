package com.group3.cafeteria_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // User is already logged in, redirect to dashboard
            return "redirect:/dashboard";
        }
        // User is not logged in, show login page
        return "redirect:/login";
    }
}