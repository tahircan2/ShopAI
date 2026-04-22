package com.shopai.repository;

import com.shopai.entity.AgentTransaction;
import com.shopai.entity.AgentTransaction.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentTransactionRepository extends JpaRepository<AgentTransaction, Long> {

    Page<AgentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<AgentTransaction> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Belirli bir kullanıcının belirli bir zaman aralığındaki işlem sayısı — limit kontrolü için.
     */
    @Query("SELECT COUNT(t) FROM AgentTransaction t WHERE t.user.id = :userId AND t.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Belirli bir kullanıcının bugünkü tamamlanmış işlem sayısı — günlük limit.
     */
    @Query("SELECT COUNT(t) FROM AgentTransaction t WHERE t.user.id = :userId AND t.status = :status AND t.createdAt >= :since")
    long countByUserIdAndStatusAndCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("since") LocalDateTime since);

    /**
     * Aktif (devam eden) işlem var mı kontrolü — aynı anda birden fazla işlem engellenir.
     */
    @Query("SELECT COUNT(t) > 0 FROM AgentTransaction t WHERE t.user.id = :userId AND t.status IN ('PENDING', 'IN_PROGRESS', 'AWAITING_APPROVAL')")
    boolean hasActiveTransaction(@Param("userId") Long userId);
}
