package com.group3.cafeteria_system.controller;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationModelAdvice {

    private final UserService userService;

    public NavigationModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addNavigationUser(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("navUser", user);
        }
    }
}
