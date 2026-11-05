package com.group3.cafeteria_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.group3.cafeteria_system.service.UserService;
import com.group3.cafeteria_system.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;  // injected from PasswordEncoderConfig
    private final UserRepository userRepository;

    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder,
                          UserRepository userRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/api/auth/**",
                                "/api/menu/**",
                                "/api/categories",
                                "/api/time-slots",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**"
                        ).permitAll()

                        .requestMatchers(
                                "/menu",
                                "/customer/menu",
                                "/cart",
                                "/api/cart/**",
                                "/confirmation",
                                "/checkout",
                                "/customer/history",
                                "/orders",
                                "/orders/**",
                                "/payment"
                        ).hasRole("STUDENT")

                        .requestMatchers(
                                "/staff/settings",
                                "/api/staff/categories",
                                "/api/staff/categories/**",
                                "/api/staff/time-slots",
                                "/api/staff/time-slots/**",
                                "/api/staff/users",
                                "/api/staff/users/**",
                                "/categories/**",
                                "/users/**",
                                "/time-slots/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/staff/**",
                                "/api/staff/**"
                        ).hasAnyRole("STAFF", "ADMIN")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )

                .addFilterBefore(new AccountStatusFilter(userRepository), AuthorizationFilter.class)

                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    if (request.getRequestURI().startsWith("/api/")) {
                                        response.setStatus(401);
                                        response.setContentType("application/json");
                                        response.getWriter().write(
                                                "{\"error\": \"Unauthorised. Please log in.\"}"
                                        );
                                    } else {
                                        response.sendRedirect("/login");
                                    }
                                })
                        .accessDeniedHandler((request, response, exception) -> {
                            boolean staff = request.isUserInRole("STAFF")
                                    || request.isUserInRole("ADMIN");
                            if (staff && !request.getRequestURI().startsWith("/api/")) {
                                response.sendRedirect("/staff/dashboard");
                            } else if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(403);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        "{\"error\": \"Forbidden. You do not have access to this resource.\"}");
                            } else {
                                response.sendError(403);
                            }
                        })
                );

        return http.build();
    }
}
