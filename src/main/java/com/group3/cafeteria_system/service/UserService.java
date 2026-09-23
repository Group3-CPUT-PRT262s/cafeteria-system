package com.group3.cafeteria_system.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
//import java.util.UUID;
import java.security.SecureRandom;

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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
    }

    /*
     * Normalise username before storing or searching.
     */
    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /*
     * Normalise email before storing or searching.
     */
    private String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase();
    }

    /*
     * Spring Security user lookup.
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername =
                normalizeUsername(username);

        User user = userRepository
                .findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: "
                                        + normalizedUsername));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole()
                        )
                )
        );
    }

    // ---------------------------------------------------------
    // User retrieval
    // ---------------------------------------------------------

    public Optional<User> findByUsername(String username) {

        return userRepository.findByUsername(
                normalizeUsername(username)
        );
    }

    /*
     * Used by controllers to get the user ID
     * for operations such as placing an order.
     */
    public Long getUserIdByUsername(String username) {

        return findByUsername(username)
                .map(User::getUserId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + username
                        ));
    }

    // ---------------------------------------------------------
    // Admin / system user creation
    // ---------------------------------------------------------

    /*
     * Used by DataInitialiser to create the initial
     * admin account.
     *
     * The raw password is encoded before being stored.
     */
    public User createUser(
            String username,
            String rawPassword,
            String firstName,
            String lastName,
            String email,
            String role) {

        String normalizedUsername =
                normalizeUsername(username);

        String normalizedEmail =
                normalizeEmail(email);

        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already taken. Please choose another one.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered.");
        }

        User user = new User();

        user.setUsername(normalizedUsername);

        user.setPasswordHash(
                passwordEncoder.encode(rawPassword)
        );

        user.setFirstName(firstName == null ? "" : firstName.trim());
        user.setLastName(lastName == null ? "" : lastName.trim());
        user.setEmail(normalizedEmail);
        user.setRole(role.toUpperCase());

        return userRepository.save(user);
    }

    // ---------------------------------------------------------
    // Customer registration
    // ---------------------------------------------------------

    /*
     * Creates a new registered user.
     *
     * The password is encoded before being stored.
     * The role should be assigned by the backend rather
     * than accepted from the registration form.
     */
    public User registerUser(
            String username,
            String rawPassword,
            String firstName,
            String lastName,
            String email,
            String role) {

        String normalizedUsername =
                normalizeUsername(username);

        String normalizedEmail =
                normalizeEmail(email);


        // Check whether username is already registered
        if (userRepository.existsByUsername(
                normalizedUsername)) {

            throw new IllegalArgumentException(
                    "Username already taken. "
                            + "Please choose another one."
            );
        }


        // Check whether email is already registered
        if (userRepository.existsByEmail(
                normalizedEmail)) {

            throw new IllegalArgumentException(
                    "Email already registered."
            );
        }


        User user = new User();

        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(rawPassword)
        );

        user.setFirstName(firstName == null ? "" : firstName.trim()
        );

        user.setLastName(lastName == null ? "" : lastName.trim()
        );

        user.setEmail(normalizedEmail);

        user.setRole(
                role.toUpperCase()
        );

        return userRepository.save(user);
    }


    /*
     * Check whether a username already exists.
     */
    public boolean userExists(String username) {

        return userRepository.existsByUsername(
                normalizeUsername(username)
        );
    }

    // ---------------------------------------------------------
    // Password reset
    // ---------------------------------------------------------

    /*
     * Generate a 6-digit password reset code for a user.
     */
    public String generatePasswordResetToken(String email) {

        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No account found with that email address."
                        )
                );

        // Remove any existing reset codes for this user
        tokenRepository.deleteByUserId(user.getUserId());

        // Generate a secure 6-digit code
        SecureRandom secureRandom = new SecureRandom();

        String token = String.format(
                "%06d", secureRandom.nextInt(1_000_000)
        );

        // Save the code
        tokenRepository.save(
                new PasswordResetToken(
                        token,
                        user.getUserId()
                )
        );

        return token;
    }


    /*
     * Verify a password reset code.
     *
     * Returns the user ID associated with the valid code.
     */
    public Long verifyPasswordResetToken(String token) {

        PasswordResetToken resetToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid or expired verification code."
                                )
                        );

        // Check whether the code is still valid
        if (!resetToken.isValid()) {

            throw new BadCredentialsException(
                    "This verification code has expired "
                            + "or has already been used. "
                            + "Please request a new code."
            );
        }

        return resetToken.getUserId();
    }


    /*
     * Reset a user's password using a valid reset code.
     */
    public void resetPassword(
            String token,
            String newPassword) {

        PasswordResetToken resetToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid or expired verification code."
                                )
                        );

        // Check whether the code is still valid
        if (!resetToken.isValid()) {

            throw new BadCredentialsException(
                    "This verification code has expired "
                            + "or has already been used. "
                            + "Please request a new code."
            );
        }

        // Find the user associated with the code
        User user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found."
                        )
                );

        // Encode and update the password
        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // Mark the reset code as used
        resetToken.setUsed(true);

        tokenRepository.save(resetToken);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found."));
        user.setRole(role);
        userRepository.save(user);
    }
}
