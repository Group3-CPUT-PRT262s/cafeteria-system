package com.group3.cafeteria_system.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.group3.cafeteria_system.model.PasswordResetToken;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.PasswordResetTokenRepository;
import com.group3.cafeteria_system.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {


    private final PasswordResetTokenRepository tokenRepository;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

public UserService (UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenRepository = tokenRepository;
}

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername = normalizeUsername(username);
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + normalizedUsername));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole())
                )
        );
    }

    // User retrieval
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username));
    }

    // New — used by controllers to get the userId for placeOrder()
    public Long getUserIdByUsername(String username) {
        return findByUsername(username)
                .map(User::getUserId)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + username));
    }

    // User creation
    // Simple version for mock data (demo phase)... resolve later(check the datainitialiser class)
    public User createUser(String username, String rawPassword, // make void later**
                           String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }
//    may need to use seeded data for checks/testing, keep blank for now.

    /*
    Full version for registration (Term 3)
    - creating/rigistr user with registration form
     */
    public User registerUser(String username, String rawPassword,
                             String firstName, String lastName,
                             String email, String role) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already taken. Please choose another one.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered.");
        }
        // Look into creating maybe custom business rule exceptions, grouped. **Nice to have code later on.(assign to Seth?)

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(normalizedEmail);
        user.setRole(role);
        return userRepository.save(user);
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(normalizeUsername(username));
    }

    // Password Reset method
    public String generatePasswordResetToken(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new RuntimeException(
                        "No account found with that email address."));
        // Remove any existing unused tokens for this user
        tokenRepository.deleteByUserId(user.getUserId());

        // Generate a unique secure token for the user
        String token = UUID.randomUUID().toString();

        // Save it
        tokenRepository.save(
                new PasswordResetToken(token, user.getUserId()));
        return token;
    }

    public void resetPassword(String token, String newPassword) {
        // Find the token record
        PasswordResetToken resetToken = tokenRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or expired reset link."));

        // Validate it
        if (!resetToken.isValid()) {
            throw new BadCredentialsException(
                    "This reset link has expired or has already been used. " + " Please request a new one.");
        }

        // Update the user's password
        User user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used — prevents reuse
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}