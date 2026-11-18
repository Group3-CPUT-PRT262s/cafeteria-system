package com.group3.cafeteria_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpSession;

import com.group3.cafeteria_system.service.EmailService;
import com.group3.cafeteria_system.service.UserService;
import com.group3.cafeteria_system.service.RegistrationService;

class AuthControllerTest {

    @Test
    void loginPageShouldExposeErrorMessageForFailedLogin() {
        UserService userService = mock(UserService.class);
        EmailService emailService = mock(EmailService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        RegistrationService registrationService = mock(RegistrationService.class);

        AuthController controller = new AuthController(
                userService,
                emailService,
                authenticationManager,
                registrationService
        );

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.loginPage("true", null, model);

        assertEquals("login", viewName);
        assertEquals(
                "Incorrect username or password. Please try again.",
                model.get("errorMessage")
        );
    }

    @Test
    void forgotPasswordFormSendsCodeAndRedirectsToCodeEntry() {
        UserService userService = mock(UserService.class);
        EmailService emailService = mock(EmailService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        AuthController controller = new AuthController(
                userService, emailService, authenticationManager, registrationService);
        HttpSession session = mock(HttpSession.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(userService.generatePasswordResetToken("student@example.com"))
                .thenReturn("123456");

        String viewName = controller.forgotPasswordForm(
                "student@example.com", session, redirectAttributes);

        assertEquals("redirect:/verify-reset-code", viewName);
        verify(emailService).sendPasswordResetEmail("student@example.com", "123456");
    }
}
