package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by their username
    // Used during login to look up the user
    Optional<User> findByUsername(String username);

    // Check if a username already exists
    // Used during registration (Term 3)
    boolean existsByUsername(String username);

    // used during registration to prevent duplicate emails
    boolean existsByEmail(String email);

    // Needed for forgot password — look up user by email
    Optional<User> findByEmail(String email);

    long countByRole(String role);

    long countByRoleAndAccountStatus(String role, String accountStatus);

    long countByRoleAndAccountStatusIsNull(String role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("update User u set u.walletBalance = :balance where u.userId = :userId")
    int updateWalletBalance(@Param("userId") Long userId,
                            @Param("balance") Double balance);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = 'ADMIN' "
            + "and (u.accountStatus = 'ACTIVE' or u.accountStatus is null)")
    List<User> lockActiveAdmins();
}