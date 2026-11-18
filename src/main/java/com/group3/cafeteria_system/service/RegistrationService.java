package com.group3.cafeteria_system.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.cafeteria_system.model.RegistrationVerification;
import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.RegistrationVerificationRepository;

@Service
public class RegistrationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
                    Pattern.CASE_INSENSITIVE);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationVerificationRepository verificationRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistrationService(RegistrationVerificationRepository verificationRepository,
                               UserService userService,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.verificationRepository = verificationRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void start(String username, String password, String firstName,
                      String lastName, String email) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new IllegalArgumentException("Username must be 3-20 characters and can only contain letters, numbers, and underscores.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (userService.userExists(normalizedUsername)) {
            throw new IllegalArgumentException("Username already taken. Please choose another one.");
        }
        if (userService.emailExists(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered.");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        verificationRepository.deleteByEmail(normalizedEmail);
        verificationRepository.save(new RegistrationVerification(
                normalizedEmail, passwordEncoder.encode(code), normalizedUsername,
                passwordEncoder.encode(password), firstName, lastName,
                LocalDateTime.now().plusMinutes(10)));
        emailService.sendRegistrationCode(normalizedEmail, code);
    }

    @Transactional(noRollbackFor = InvalidVerificationException.class)
    public User verify(String email, String code) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedCode = code == null ? "" : code.trim();
        if (!normalizedCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Enter the 6-digit verification code.");
        }

        RegistrationVerification verification = verificationRepository
                .findByEmailForUpdate(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending registration was found. Please register again."));
        if (verification.isExpired()) {
            verificationRepository.delete(verification);
            throw new InvalidVerificationException(
                    "The verification code has expired. Please register again.");
        }
        if (!passwordEncoder.matches(normalizedCode, verification.getCodeHash())) {
            int attempts = verification.getAttempts() + 1;
            verification.setAttempts(attempts);
            if (attempts >= 5) {
                verificationRepository.delete(verification);
                throw new InvalidVerificationException(
                        "Too many incorrect codes. Please register again.");
            }
            throw new InvalidVerificationException("The verification code is incorrect.");
        }

        User user = userService.registerVerifiedUser(
                verification.getUsername(), verification.getPasswordHash(),
                verification.getFirstName(), verification.getLastName(), verification.getEmail());
        verificationRepository.delete(verification);
        return user;
    }

    private static final class InvalidVerificationException extends IllegalArgumentException {
        private InvalidVerificationException(String message) {
            super(message);
        }
    }
}
