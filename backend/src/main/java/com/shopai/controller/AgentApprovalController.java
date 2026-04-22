package com.shopai.controller;

import com.shopai.dto.request.AgentRequests.CreateApprovalRequest;
import com.shopai.dto.response.AgentResponses.ApprovalActionResponse;
import com.shopai.dto.response.AgentResponses.ApprovalResponse;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.UserRepository;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.AgentApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent/approvals")
@RequiredArgsConstructor
@Tag(name = "Agent Approval", description = "AI Agent işlemlerinin kullanıcı tarafından onaylanması")
public class AgentApprovalController {

    private final AgentApprovalService approvalService;
    private final UserRepository userRepository;

    @PostMapping("/{token}/approve")
    @Operation(summary = "İşlem planını onayla")
    public ResponseEntity<ApprovalActionResponse> approve(@AuthenticationPrincipal JwtAuthDetails auth,
                                                        @PathVariable String token) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(approvalService.approve(user, token));
    }

    @PostMapping("/{token}/reject")
    @Operation(summary = "İşlem planını reddet")
    public ResponseEntity<ApprovalActionResponse> reject(@AuthenticationPrincipal JwtAuthDetails auth,
                                                       @PathVariable String token) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(approvalService.reject(user, token));
    }

    @GetMapping("/{token}/status")
    @Operation(summary = "Belirli bir onayın durumunu ve kalan süresini getir")
    public ResponseEntity<ApprovalResponse> getStatus(@AuthenticationPrincipal JwtAuthDetails auth,
                                                    @PathVariable String token) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(approvalService.getStatus(user, token));
    }

    @GetMapping("/pending")
    @Operation(summary = "Kullanıcının bekleyen tüm onaylarını listele")
    public ResponseEntity<List<ApprovalResponse>> getPending(@AuthenticationPrincipal JwtAuthDetails auth) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(approvalService.getPendingApprovals(user));
    }

    @PostMapping("/create")
    @Operation(summary = "Yeni onay isteği oluştur (Dahili Kullanım)")
    public ResponseEntity<ApprovalResponse> create(@AuthenticationPrincipal JwtAuthDetails auth,
                                                 @Valid @RequestBody CreateApprovalRequest req) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(approvalService.createApproval(user, req));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }
}
