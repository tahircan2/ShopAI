package com.shopai.repository;

import com.shopai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, @Param("role") User.Role role, Pageable pageable);

    Optional<User> findByEmailVerifyToken(String token);
    Optional<User> findByPasswordResetToken(String token);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
    void resetFailedAttempts(Long id);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :time WHERE u.id = :id")
    void updateLastLogin(Long id, LocalDateTime time);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.lockedUntil < :now")
    int unlockExpiredAccounts(LocalDateTime now);
}
