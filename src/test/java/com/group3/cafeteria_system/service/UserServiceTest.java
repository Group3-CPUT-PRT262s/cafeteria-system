package com.group3.cafeteria_system.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.group3.cafeteria_system.model.User;
import com.group3.cafeteria_system.repository.PasswordResetTokenRepository;
import com.group3.cafeteria_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, tokenRepository);
    }

    @Test
    void preventsAdminFromDemotingTheirOwnAccount() {
        User admin = user(1L, "ADMIN");
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(admin));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class,
                () -> userService.updateUserRole(1L, 1L, "STAFF"));
    }

    @Test
    void protectsTheLastActiveAdministratorFromDeactivation() {
        User admin = user(1L, "ADMIN");
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(admin));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndAccountStatus("ADMIN", "ACTIVE")).thenReturn(1L);
        when(userRepository.countByRoleAndAccountStatusIsNull("ADMIN")).thenReturn(0L);

        assertThrows(IllegalStateException.class,
                () -> userService.updateAccountStatus(2L, 1L, "SUSPENDED"));
    }

    @Test
    void allowsAdminToPromoteAnotherAccount() {
        User admin = user(1L, "ADMIN");
        User staff = user(2L, "STAFF");
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(admin));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(staff));
        when(userRepository.save(staff)).thenReturn(staff);

        userService.updateUserRole(1L, 2L, "ADMIN");

        verify(userRepository).save(staff);
    }

    private User user(Long id, String role) {
        User user = new User("user" + id, "encoded", role);
        user.setUserId(id);
        return user;
    }
}
