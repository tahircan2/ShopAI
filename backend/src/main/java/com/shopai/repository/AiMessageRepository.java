package com.shopai.repository;

import com.shopai.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
