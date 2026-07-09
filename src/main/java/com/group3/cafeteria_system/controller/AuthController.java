package com.group3.cafeteria_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.group3.cafeteria_system.service.EmailService;
import com.group3.cafeteria_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;

    public AuthController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    /* LOGIN
    -  POST /login is handled entirely by Spring Security.
    - This controller only serves the login page (GET).
    */

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage",
                    "Incorrect username or password. " +
                            "Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage",
                    "You have been logged out successfully.");
        }
        return "login";
    }

    // ─────────────────────────────────────────
    // REGISTRATION
    // ─────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // REST — frontend submits JSON, receives JSON
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> body) {

        String username  = body.getOrDefault("username", "").trim();
        String password  = body.getOrDefault("password", "");
        String firstName = body.getOrDefault("firstName", "").trim();
        String lastName  = body.getOrDefault("lastName", "").trim();
        String email     = body.getOrDefault("email", "").trim();

        // Validation
        if (username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username is required."));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Password must be at least 8 characters."));
        }
        if (!email.contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "A valid email address is required."));
        }

        try {
            userService.registerUser(
                    username, password,
                    firstName, lastName,
                    email, "STUDENT"
            );
            return ResponseEntity.ok(Map.of(
                    "message",
                    "Account created successfully. You can now log in.",
                    "username", username
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Thymeleaf — browser form submits, gets redirect
    @PostMapping("/register")
    public String registerForm(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password must be at least 8 characters.");
            return "redirect:/register";
        }

        try {
            userService.registerUser(
                    username, password,
                    firstName, lastName,
                    email, "STUDENT"
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Account created! You can now log in.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    // ─────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    // Thymeleaf form
    @PostMapping("/forgot-password")
    public String forgotPasswordForm(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            String token = userService.generatePasswordResetToken(email);
            emailService.sendPasswordResetEmail(email, token);
        } catch (RuntimeException ignored) {
            // Swallow — never confirm whether email exists
        }

        // Always show the same message regardless of outcome
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "If an account exists with that email, " +
                        "a reset link has been sent. " +
                        "Please check your inbox.");
        return "redirect:/forgot-password";
    }

    // REST
    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> forgotPasswordApi(
            @RequestBody Map<String, String> body) {

        String email = body.getOrDefault("email", "").trim();

        try {
            String token =
                    userService.generatePasswordResetToken(email);
            emailService.sendPasswordResetEmail(email, token);
        } catch (RuntimeException ignored) {
            // Same — always return vague response
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "If an account exists with that email, " +
                        "a reset link has been sent."
        ));
    }


    /* RESET PASSWORD
     User arrives here via link in their email
     */

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(required = false) String token,
            Model model) {

        if (token == null || token.isBlank()) {
            model.addAttribute("errorMessage",
                    "Invalid reset link. Please request a new one.");
            return "auth/reset-password";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    // Thymeleaf form
    @PostMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password must be at least 8 characters.");
            return "redirect:/reset-password?token=" + token;
        }

        try {
            userService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Password reset successfully. You can now log in.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }

    // REST
    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resetPasswordApi(
            @RequestBody Map<String, String> body) {

        String token    = body.getOrDefault("token", "");
        String password = body.getOrDefault("password", "");

        if (password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Password must be at least 8 characters."));
        }

        try {
            userService.resetPassword(token, password);
            return ResponseEntity.ok(Map.of(
                    "message",
                    "Password reset successfully. You can now log in."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
