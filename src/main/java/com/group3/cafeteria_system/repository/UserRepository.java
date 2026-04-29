package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by their username
    // Used during login to look up the user
    Optional<User> findByUsername(String username);

    // Check if a username already exists
    // Used during registration (Term 3)
    boolean existsByUsername(String username);
}