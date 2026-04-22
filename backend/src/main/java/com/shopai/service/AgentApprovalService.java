package com.shopai.service;

import com.shopai.dto.request.AgentRequests.CreateApprovalRequest;
import com.shopai.dto.response.AgentResponses.ApprovalActionResponse;
import com.shopai.dto.response.AgentResponses.ApprovalResponse;
import com.shopai.entity.PendingApproval;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.PendingApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentApprovalService {

    private final PendingApprovalRepository pendingApprovalRepository;
    private final AgentSecurityService securityService;

    @Transactional
    public ApprovalResponse createApproval(User user, CreateApprovalRequest request) {
        // Limit kontrolleri
        securityService.checkDailyLimit(user.getId());
        securityService.checkHourlyLimit(user.getId());

        PendingApproval approval = securityService.createApprovalToken(
                user, request.getPlanData(), request.getAgentType(), request.getSessionId());

        return mapToResponse(approval);
    }

    @Transactional
    public ApprovalActionResponse approve(User user, String token) {
        PendingApproval approval = getPendingApproval(user, token);
        
        approval.setStatus(PendingApproval.ApprovalStatus.APPROVED);
        approval.setRespondedAt(LocalDateTime.now());
        
        return ApprovalActionResponse.builder()
                .status("APPROVED")
                .message("İşlem planı onaylandı")
                .build();
    }

    @Transactional
    public ApprovalActionResponse reject(User user, String token) {
        PendingApproval approval = getPendingApproval(user, token);
        
        approval.setStatus(PendingApproval.ApprovalStatus.REJECTED);
        approval.setRespondedAt(LocalDateTime.now());
        
        return ApprovalActionResponse.builder()
                .status("REJECTED")
                .message("İşlem planı reddedildi")
                .build();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse getStatus(User user, String token) {
        PendingApproval approval = pendingApprovalRepository.findByApprovalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found"));
                
        // Ensure user owns this requested approval
        if (!approval.getUser().getId().equals(user.getId())) {
             throw new ResourceNotFoundException("Approval not found");
        }
        
        return mapToResponse(approval);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getPendingApprovals(User user) {
        return pendingApprovalRepository.findActivePendingByUserId(user.getId(), LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalResponse> getLatestActiveApproval(User user) {
        return pendingApprovalRepository.findLatestActiveByUserId(user.getId(), LocalDateTime.now())
                .stream()
                .findFirst()
                .map(this::mapToResponse);
    }

    /**
     * Her 5 dakikada bir süresi geçmiş PENDING onayları EXPIRED yapar.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredApprovals() {
        int count = pendingApprovalRepository.expireOldApprovals(LocalDateTime.now());
        if (count > 0) {
            // log handled inside repository or explicitly if log is added
        }
    }

    private PendingApproval getPendingApproval(User user, String token) {
        PendingApproval approval = pendingApprovalRepository.findByApprovalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Onay kaydı bulunamadı"));

        if (!approval.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bu onay size ait değil");
        }

        if (approval.isExpired()) {
            throw new IllegalStateException("Bu onayın süresi dolmuş");
        }

        if (approval.getStatus() != PendingApproval.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Bu işlem zaten yanıtlanmış: " + approval.getStatus());
        }

        return approval;
    }

    private ApprovalResponse mapToResponse(PendingApproval approval) {
        long remaining = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), approval.getExpiresAt()));
        
        return ApprovalResponse.builder()
                .id(approval.getId())
                .approvalToken(approval.getApprovalToken())
                .planData(approval.getPlanData())
                .planHash(approval.getPlanHash())
                .agentType(approval.getAgentType())
                .status(approval.getStatus())
                .expiresAt(approval.getExpiresAt())
                .respondedAt(approval.getRespondedAt())
                .createdAt(approval.getCreatedAt())
                .remainingSeconds(remaining)
                .build();
    }
}
