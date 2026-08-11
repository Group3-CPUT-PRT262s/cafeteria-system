package com.group3.cafeteria_system.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group3.cafeteria_system.service.EmailService;
import com.group3.cafeteria_system.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";

    private final UserService userService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, EmailService emailService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
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
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE,
                    "Incorrect username or password. " +
                            "Please try again.");
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

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(required = false) String token,
            Model model) {

        if (token == null || token.isBlank()) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE,
                    "Invalid reset link. Please request a new one.");
        } else {
            model.addAttribute("token", token);
        }
        return "reset-password";
    }

    /* -------------------------------------------
     REST API ROUTES
     These return JSON — used by Postman and
     eventually by the frontend JavaScript
     
     ─────────────────────────────────────────
     LOGIN
     POST /api/auth/login
     ─────────────────────────────────────────
     Spring Security handles POST /login for
     browser form submissions. This endpoint
     handles JSON login for API/Postman testing.
    
     Postman test:
     POST /api/auth/login
     Body: { "username": "student1",
             "password": "test123" }
    
     Success: 200 + session cookie set
     Failure: 401 + error message
    */ 

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        // Basic input check
        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error("Username and password are required."));
        }

        try {
            // Authenticate against the database
            Authentication auth =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    username, password));

            // Store in security context
            SecurityContextHolder.getContext()
                    .setAuthentication(auth);

            // Bind to HTTP session so subsequent
            // requests in Postman are authenticated
            session.setAttribute(
                    HttpSessionSecurityContextRepository
                            .SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            // Extract role for the response
            String role = auth.getAuthorities()
                    .iterator().next()
                    .getAuthority()
                    .replace("ROLE_", "");

            // Build response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message",    "Login successful.");
            response.put("username",   username);
            response.put("role",       role);
            response.put("sessionId",  session.getId());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(error(
                            "Invalid username or password."));
        }
    }

    // ─────────────────────────────────────────
    // LOGOUT
    // POST /api/auth/logout
    // ─────────────────────────────────────────
    // Invalidates the current session.
    //
    // Postman test:
    // POST /api/auth/logout (no body is needed)
    //
    // Success: 200 + session cleared
    // ─────────────────────────────────────────

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logout(
            HttpSession session) {

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        // Invalidate the HTTP session
        session.invalidate();

        return ResponseEntity.ok(
                success("Logged out successfully."));
    }

    // ─────────────────────────────────────────
    // REGISTER
    // POST /api/auth/register
    // ─────────────────────────────────────────
    // Creates a new STUDENT account.
    //
    // Postman test:
    // POST /api/auth/register
    // Body: {
    //   "username":  "newuser",
    //   "password":  "password123",
    //   "firstName": "Jane",
    //   "lastName":  "Doe",
    //   "email":     "jane@example.com"
    // }
    //
    // Success: 201 Created
    // Failure: 400 Bad Request + error message
    // ─────────────────────────────────────────

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> body) {

        String username  = body.getOrDefault(
                "username",  "").trim();
        String password  = body.getOrDefault(
                "password",  "");
        String confirmPassword = body.getOrDefault(
                "confirmPassword", "");
        String firstName = body.getOrDefault(
                "firstName", "").trim();
        String lastName  = body.getOrDefault(
                "lastName",  "").trim();
        String email     = body.getOrDefault(
                "email",     "").trim();

        // ── Validation ────────────────────────
        if (username.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error("Username is required."));
        }
        if (username.length() < 3) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(
                            "Username must be at least 3 characters."));
        }
        if (password.length() < 8) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(
                            "Password must be at least 8 characters."));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error("Passwords do not match."));
        }
        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(
                            "A valid email address is required."));
        }

        try {
            userService.registerUser(
                    username, password,
                    firstName, lastName,
                    email, "STUDENT"
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message",
                    "Account created successfully. " +
                            "You can now log in.");
            response.put("username", username);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(e.getMessage()));
        }
    }

    // Also handle the Thymeleaf form POST for register
    // so the browser register page works too
    @PostMapping("/register")
    public String registerForm(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE,
                    "Password must be at least 8 characters.");
            return "redirect:/register";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE,
                    "Passwords do not match.");
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
                    ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
            return "redirect:/register";
        }
    }

    /* ─────────────────────────────────────────
     FORGOT PASSWORD
     POST /api/auth/forgot-password
     ─────────────────────────────────────────
     Generates a reset token and emails it.
     Always returns the same response whether
     the email exists or not — security best
     practice so you can't enumerate accounts.
    
     Postman test:
     POST /api/auth/forgot-password
     Body: { "email": "jane@example.com" }
    
     Always: 200 + vague success message
     Check Gmail for the reset link
    */
    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> body) {

        String email = body.getOrDefault("email", "").trim();

        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error("A valid email address is required."));
        }

        try {
            String token =
                    userService.generatePasswordResetToken(email);
            emailService.sendPasswordResetEmail(email, token);
        } catch (RuntimeException ignored) {
            // Swallow — never reveal whether email exists
        }

        // Always return this exact message
        return ResponseEntity.ok(success(
                "If an account exists with that email address, " +
                        "a password reset link has been sent. " +
                        "Please check your inbox."));
    }

    // Thymeleaf form handler for browser forgot-password page
    @PostMapping("/forgot-password")
    public String forgotPasswordForm(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            String token =
                    userService.generatePasswordResetToken(email);
            emailService.sendPasswordResetEmail(email, token);
        } catch (RuntimeException ignored) {
            // Swallow
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "If an account exists with that email, " +
                        "a reset link has been sent. " +
                        "Please check your inbox.");

        return "redirect:/forgot-password";
    }

    // ─────────────────────────────────────────
    // RESET PASSWORD
    // POST /api/auth/reset-password
    // ─────────────────────────────────────────
    // Validates the token from the email link
    // and sets a new password.
    //
    // Postman test:
    // POST /api/auth/reset-password
    // Body: {
    //   "token":           "uuid-from-email",
    //   "password":        "newpassword123",
    //   "confirmPassword": "newpassword123"
    // }
    //
    // Success: 200
    // Failure: 400 (expired, used, or mismatch)
    // ─────────────────────────────────────────

    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> body) {

        String token           = body.getOrDefault(
                "token",           "").trim();
        String password        = body.getOrDefault(
                "password",        "");
        String confirmPassword = body.getOrDefault(
                "confirmPassword", "");

        // ── Validation ────────────────────────
        if (token.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(
                            "Reset token is missing. " +
                                    "Please use the link from your email."));
        }
        if (password.length() < 8) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(
                            "Password must be at least 8 characters."));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error("Passwords do not match."));
        }

        try {
            userService.resetPassword(token, password);
            return ResponseEntity.ok(success(
                    "Password reset successfully. " +
                            "You can now log in with your new password."));

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(e.getMessage()));
        }
    }

    // Thymeleaf form handler for browser reset-password page
    @PostMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE, "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE,
                    "Password must be at least 8 characters.");
            return "redirect:/reset-password?token=" + token;
        }

        try {
            userService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Password reset successfully. " +
                            "You can now log in.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }

    // ═════════════════════════════════════════
    // PRIVATE HELPERS
    // Keeps response building consistent
    // ═════════════════════════════════════════

    private Map<String, Object> success(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",  "success");
        response.put("message", message);
        return response;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",  "error");
        response.put("message", message);
        return response;
    }
}
