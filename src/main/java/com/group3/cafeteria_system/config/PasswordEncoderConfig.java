package com.group3.cafeteria_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    // Moved out of SecurityConfig to break the circular
    // dependency between SecurityConfig and UserService.
    // UserService needs PasswordEncoder.
    // SecurityConfig needs UserService.
    // Having PasswordEncoder in SecurityConfig caused a cycle.
    // Now it lives independently — both can use it freely.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}