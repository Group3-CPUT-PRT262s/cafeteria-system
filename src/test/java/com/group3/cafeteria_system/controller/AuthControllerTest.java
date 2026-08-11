package com.group3.cafeteria_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.ui.ExtendedModelMap;

import com.group3.cafeteria_system.service.EmailService;
import com.group3.cafeteria_system.service.UserService;

class AuthControllerTest {

    @Test
    void loginPageShouldExposeErrorMessageForFailedLogin() {
        UserService userService = mock(UserService.class);
        EmailService emailService = mock(EmailService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

        AuthController controller = new AuthController(
                userService,
                emailService,
                authenticationManager
        );

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.loginPage("true", null, model);

        assertEquals("login", viewName);
        assertEquals(
                "Incorrect username or password. Please try again.",
                model.get("errorMessage")
        );
    }
}
