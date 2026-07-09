package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.group3.cafeteria_system.model.PasswordResetToken;
import com.group3.cafeteria_system.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import java.util.*;

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

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),  // changed from getPassword()
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole())
                )
        );
    }

    // User retrieval
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
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
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken, user has to try a new one.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered.");
        }
        // Look into creating maybe custom business rule exceptions, grouped. **Nice to have code later on.(assign to Seth?)

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // Password Reset method
    public String generatePasswordResetToken(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
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