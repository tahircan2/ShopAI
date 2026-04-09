package com.shopai.repository;

import com.shopai.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false, us.logoutAt = :now WHERE us.user.id = :userId AND us.isActive = true")
    void deactivateAllUserSessions(Long userId, LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.expiresAt < :now AND us.isActive = true")
    int deactivateExpiredSessions(LocalDateTime now);
}
