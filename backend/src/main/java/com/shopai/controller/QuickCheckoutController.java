package com.shopai.controller;

import com.shopai.dto.request.AgentRequests.QuickCheckoutExecuteRequest;
import com.shopai.dto.request.AgentRequests.QuickCheckoutValidateRequest;
import com.shopai.dto.response.AgentResponses.QuickCheckoutResponse;
import com.shopai.dto.response.AgentResponses.QuickCheckoutValidationResponse;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.UserRepository;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.QuickCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/quick-checkout")
@RequiredArgsConstructor
@Tag(name = "Agent Quick Checkout", description = "AI asistan kontrolünde hızlı ödeme ve sepet işleme")
public class QuickCheckoutController {

    private final QuickCheckoutService quickCheckoutService;
    private final UserRepository userRepository;

    @PostMapping("/validate")
    @Operation(summary = "Hızlı ödeme öncesi sepet ve stok validasyonu (Agent için)")
    public ResponseEntity<QuickCheckoutValidationResponse> validate(@AuthenticationPrincipal JwtAuthDetails auth,
                                                                  @RequestBody QuickCheckoutValidateRequest req) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(quickCheckoutService.validateCheckout(user, req));
    }

    @PostMapping("/execute")
    @Operation(summary = "Hızlı ödemeyi gerçekleştir (Onay tokenı ile)")
    public ResponseEntity<QuickCheckoutResponse> execute(@AuthenticationPrincipal JwtAuthDetails auth,
                                                       @Valid @RequestBody QuickCheckoutExecuteRequest req,
                                                       HttpServletRequest request) {
        User user = findUser(auth.getUserId());
        return ResponseEntity.ok(quickCheckoutService.executeCheckout(user, req, request));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }
}
