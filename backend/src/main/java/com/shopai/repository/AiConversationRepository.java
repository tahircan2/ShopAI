package com.shopai.repository;

import com.shopai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findBySessionIdAndUserId(String sessionId, Long userId);
    Optional<AiConversation> findBySessionId(String sessionId);
}
