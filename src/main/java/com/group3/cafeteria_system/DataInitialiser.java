package com.group3.cafeteria_system;

import com.group3.cafeteria_system.service.UserService;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitialiser implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitialiser.class);

    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789" +
                    "!@#$%^&*";

    private static final SecureRandom random = new SecureRandom();

    private final UserService userService;

    public DataInitialiser(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {

        /*
         * Creates the main admin account if one does not already exist.
         * The generated password is displayed in the application console.
         * NOTE: an admin account will not be created after each run if an admin account is present in our/your database
         */
        if (!userService.userExists("admin")) {

            String generatedPassword = generatePassword();

            userService.createUser(
                    "admin",
                    generatedPassword,
                    "ADMIN"
            );

            logger.info("""
                    ========================================
                    Springboot application run detected.

                    Admin account created:
                      Username: admin
                      Password: {}

                      CHANGE THIS PASSWORD IMMEDIATELY (leave for testing)

                    Log in at: http://localhost:8080/login
                    ========================================
                    """, generatedPassword);
        }
    }

    private String generatePassword() {

        StringBuilder password = new StringBuilder(16);

        for (int i = 0; i < 16; i++) {
            int index = random.nextInt(PASSWORD_CHARACTERS.length());
            password.append(PASSWORD_CHARACTERS.charAt(index));
        }

        return password.toString();
    }
}