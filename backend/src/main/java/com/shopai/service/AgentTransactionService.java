package com.shopai.service;

import com.shopai.entity.AgentTransaction;
import com.shopai.entity.AgentTransaction.TransactionStatus;
import com.shopai.entity.AgentTransaction.TransactionType;
import com.shopai.entity.AgentTransactionStep;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.AgentTransactionRepository;
import com.shopai.repository.AgentTransactionStepRepository;
import com.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTransactionService {

    private final AgentTransactionRepository transactionRepository;
    private final AgentTransactionStepRepository stepRepository;
    private final UserRepository userRepository;

    @Transactional
    public AgentTransaction startTransaction(Long userId, String sessionId, TransactionType type, int totalSteps) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AgentTransaction tx = AgentTransaction.builder()
                .user(user)
                .sessionId(sessionId)
                .transactionType(type)
                .status(TransactionStatus.IN_PROGRESS)
                .totalSteps(totalSteps)
                .completedSteps(0)
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public void addStep(Long transactionId, int order, AgentTransactionStep.StepType type, String description, Map<String, Object> requestData) {
        AgentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        AgentTransactionStep step = AgentTransactionStep.builder()
                .transaction(tx)
                .stepOrder(order)
                .stepType(type)
                .stepDescription(description)
                .status(AgentTransactionStep.StepStatus.IN_PROGRESS)
                .requestData(requestData)
                .build();

        stepRepository.save(step);
    }

    @Transactional
    public void completeStep(Long transactionId, int order, Map<String, Object> responseData, long durationMs) {
        AgentTransactionStep step = stepRepository.findByTransactionIdAndStepOrder(transactionId, order)
                .orElseThrow(() -> new ResourceNotFoundException("Step not found"));

        step.setStatus(AgentTransactionStep.StepStatus.COMPLETED);
        step.setResponseData(responseData);
        step.setDurationMs(durationMs);
        stepRepository.save(step);

        AgentTransaction tx = step.getTransaction();
        tx.setCompletedSteps(tx.getCompletedSteps() + 1);
        if (tx.getCompletedSteps().equals(tx.getTotalSteps())) {
            tx.setStatus(TransactionStatus.COMPLETED);
        }
        transactionRepository.save(tx);
    }

    @Transactional
    public void failTransaction(Long transactionId, String errorMessage) {
        AgentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        tx.setStatus(TransactionStatus.FAILED);
        tx.setErrorMessage(errorMessage);
        transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public Page<AgentTransaction> getHistory(Long userId, int page, int size) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<AgentTransactionStep> getSteps(Long transactionId) {
        return stepRepository.findByTransactionIdOrderByStepOrderAsc(transactionId);
    }

    @Transactional
    public void submitFeedback(Long transactionId, Long userId, Integer score, String text) {
        AgentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!tx.getUser().getId().equals(userId)) {
            throw new com.shopai.exception.ForbiddenException("Bu işleme geri bildirim verme yetkiniz yok.");
        }

        tx.setFeedbackScore(score);
        tx.setFeedbackText(text);
        transactionRepository.save(tx);
    }
}
