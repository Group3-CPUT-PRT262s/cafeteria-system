package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    // UserDetailsService is a Spring Security interface.
    // Implementing it tells Spring Security how to load
    // a user when someone tries to log in.

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── Spring Security method ────────────────────
    // Spring Security calls this automatically during login.
    // It loads the user from the database by username.
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole())
                )
        );
    }

    // ── User retrieval ────────────────────────────
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // ── User creation (used for seeding test users) ──
    public User createUser(String username, String rawPassword,
                           String role) {
        User user = new User();
        user.setUsername(username);
        // Encode the password before saving — never store plain text
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    // Check if a user already exists
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}