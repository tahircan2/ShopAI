package com.shopai.repository;

import com.shopai.entity.AgentTransactionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentTransactionStepRepository extends JpaRepository<AgentTransactionStep, Long> {

    List<AgentTransactionStep> findByTransactionIdOrderByStepOrderAsc(Long transactionId);
    
    java.util.Optional<AgentTransactionStep> findByTransactionIdAndStepOrder(Long transactionId, int stepOrder);
}
