package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

public UserService (UserRepository userRepository,  PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
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
    // Simple version for seed data (demo phase)
//    public User createUser(String username, String rawPassword,
//                           String role) {
//        User user = new User();
//        user.setUsername(username);
//        user.setPasswordHash(passwordEncoder.encode(rawPassword));
//        user.setRole(role);
//        return userRepository.save(user);
//    } may need to use seeded data for checks/testing, keep blank for now.

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
}