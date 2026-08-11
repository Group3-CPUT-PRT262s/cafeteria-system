package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // Removes old tokens when user requests a new one
    void deleteByUserId(Long userId);
}