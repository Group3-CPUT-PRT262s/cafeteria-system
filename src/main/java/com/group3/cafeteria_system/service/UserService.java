package com.group3.cafeteria_system.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
//import java.util.UUID;
import java.security.SecureRandom;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.PasswordResetToken;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.PasswordResetTokenRepository;
import com.group3.cafeteria_system.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
                    Pattern.CASE_INSENSITIVE);
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository tokenRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.walletService = walletService;
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
                "ACTIVE".equals(user.getAccountStatus()),
                true,
                true,
                true,
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole()
                        ))
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

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
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
    @Transactional
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

        if (!normalizedUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Username must be 3-20 characters and can only contain letters, numbers, and underscores.");
        }

        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!List.of("STUDENT", "STAFF", "ADMIN").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role must be STUDENT, STAFF, or ADMIN.");
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
        user.setRole(normalizedRole);

        User saved = userRepository.save(user);
        walletService.initializeWallet(saved.getUserId());
        return saved;
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
    @Transactional
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

        if (!normalizedUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Username must be 3-20 characters and can only contain letters, numbers, and underscores.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!List.of("STUDENT", "STAFF", "ADMIN").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role must be STUDENT, STAFF, or ADMIN.");
        }

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

        user.setRole(normalizedRole);

        User saved = userRepository.save(user);
        walletService.initializeWallet(saved.getUserId());
        return saved;
    }


    /*
     * Check whether a username already exists.
     */
    public boolean userExists(String username) {

        return userRepository.existsByUsername(
                normalizeUsername(username)
        );
    }

    public User updateProfile(String username, String firstName,
                              String lastName, String email) {
        return updateProfile(username, firstName, lastName, email, null);
    }

    public User updateProfile(String username, String firstName,
                              String lastName, String email, String profilePictureUrl) {
        User user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));

        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already registered.");
                });

        user.setFirstName(firstName == null ? "" : firstName.trim());
        user.setLastName(lastName == null ? "" : lastName.trim());
        user.setEmail(normalizedEmail);
        if (profilePictureUrl != null) {
            user.setProfilePictureUrl(profilePictureUrl);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User registerVerifiedUser(String username, String passwordHash,
                                     String firstName, String lastName, String email) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        if (!normalizedUsername.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Username must be 3-20 characters and can only contain letters, numbers, and underscores.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already taken. Please choose another one.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered.");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName == null ? "" : firstName.trim());
        user.setLastName(lastName == null ? "" : lastName.trim());
        user.setEmail(normalizedEmail);
        user.setRole("STUDENT");
        user.setWalletBalance(0.0);
        User saved = userRepository.save(user);
        walletService.initializeWallet(saved.getUserId());
        return saved;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
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

    @Transactional
    public void updateUserRole(Long requesterId, Long userId, String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!List.of("STUDENT", "STAFF", "ADMIN").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role must be STUDENT, STAFF, or ADMIN.");
        }
        userRepository.lockActiveAdmins();
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (requesterId.equals(userId) && !"ADMIN".equals(normalizedRole)) {
            throw new IllegalStateException("You cannot remove your own administrator access.");
        }
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(normalizedRole)
                && isLastActiveAdmin(user)) {
            throw new IllegalStateException("The last active administrator cannot be demoted.");
        }
        user.setRole(normalizedRole);
        userRepository.save(user);
    }

    @Transactional
    public void updateAccountStatus(Long requesterId, Long userId, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "INACTIVE", "SUSPENDED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be ACTIVE, INACTIVE, or SUSPENDED.");
        }
        userRepository.lockActiveAdmins();
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (requesterId.equals(userId) && !"ACTIVE".equals(normalizedStatus)) {
            throw new IllegalStateException("You cannot deactivate or suspend your own account.");
        }
        if ("ADMIN".equals(user.getRole())
                && "ACTIVE".equals(user.getAccountStatus())
                && !"ACTIVE".equals(normalizedStatus)
                && isLastActiveAdmin(user)) {
            throw new IllegalStateException("The last active administrator cannot be deactivated.");
        }
        user.setAccountStatus(normalizedStatus);
        userRepository.save(user);
    }

    private boolean isLastActiveAdmin(User target) {
        long activeAdmins = userRepository.countByRoleAndAccountStatus("ADMIN", "ACTIVE")
                + userRepository.countByRoleAndAccountStatusIsNull("ADMIN");
        return "ACTIVE".equals(target.getAccountStatus()) && activeAdmins <= 1;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
        if (user.isAdmin()) {
            throw new IllegalStateException(
                    "Administrator accounts cannot be deleted from the profile page.");
        }

        user.setAccountStatus("INACTIVE");
        userRepository.save(user);
        tokenRepository.deleteByUserId(userId);
    }
}
