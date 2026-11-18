package com.group3.cafeteria_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group3.cafeteria_system.model.RegistrationVerification;
import jakarta.persistence.LockModeType;

public interface RegistrationVerificationRepository
        extends JpaRepository<RegistrationVerification, Long> {

    Optional<RegistrationVerification> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from RegistrationVerification v where v.email = :email")
    Optional<RegistrationVerification> findByEmailForUpdate(@Param("email") String email);

    @Modifying
    @Query("delete from RegistrationVerification v where v.email = :email")
    int deleteByEmail(@Param("email") String email);
}
