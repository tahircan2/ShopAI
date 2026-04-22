package com.shopai.repository;

import com.shopai.entity.PendingApproval;
import com.shopai.entity.PendingApproval.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {

    Optional<PendingApproval> findByApprovalToken(String approvalToken);

    List<PendingApproval> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ApprovalStatus status);

    /**
     * Süresi dolmuş PENDING onayları otomatik EXPIRED olarak işaretle.
     */
    @Modifying
    @Query("UPDATE PendingApproval p SET p.status = 'EXPIRED' WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    int expireOldApprovals(@Param("now") LocalDateTime now);

    /**
     * Kullanıcının bekleyen onayları.
     */
    @Query("SELECT p FROM PendingApproval p WHERE p.user.id = :userId AND p.status = 'PENDING' AND p.expiresAt > :now ORDER BY p.createdAt DESC")
    List<PendingApproval> findActivePendingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Kullanıcının son aktif (PENDING veya APPROVED) onayını getirir.
     */
    @Query("SELECT p FROM PendingApproval p WHERE p.user.id = :userId AND (p.status = 'PENDING' OR p.status = 'APPROVED') AND p.expiresAt > :now ORDER BY p.createdAt DESC")
    List<PendingApproval> findLatestActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
