package com.shopai.controller;

import com.shopai.dto.request.AgentRequests.AgentFeedbackRequest;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.AgentTransactionService;
import com.shopai.dto.response.AgentResponses.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent/transactions")
@RequiredArgsConstructor
@Tag(name = "Agent Transaction", description = "AI Agent işlemlerinin izlenmesi ve geçmişi")
public class AgentTransactionController {

    private final AgentTransactionService transactionService;

    @GetMapping
    @Operation(summary = "Kullanıcının AI işlem geçmişini getir")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(@AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getHistory(auth.getUserId(), page, size)
                .map(TransactionResponse::fromEntity));
    }

    @GetMapping("/history")
    @Operation(summary = "Kullanıcının AI işlem geçmişini getir (alias)")
    public ResponseEntity<Page<TransactionResponse>> getHistory(@AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return getTransactions(auth, page, size);
    }

    @GetMapping("/{id}/steps")
    @Operation(summary = "İşlemin detaylı adımlarını getir")
    public ResponseEntity<List<TransactionStepResponse>> getSteps(@AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getSteps(id).stream()
                .map(TransactionStepResponse::fromEntity)
                .toList());
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Tamamlanan işleme geri bildirim ver")
    public ResponseEntity<Void> submitFeedback(@AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long id,
            @Valid @RequestBody AgentFeedbackRequest req) {
        transactionService.submitFeedback(id, auth.getUserId(), req.getScore(), req.getFeedbackText());
        return ResponseEntity.ok().build();
    }
}
