package com.shopai.controller;

import com.shopai.dto.request.AgentRequests.UpdateAiPreferenceRequest;

import com.shopai.security.JwtAuthDetails;
import com.shopai.service.UserAiPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/ai-preferences")
@RequiredArgsConstructor
@Tag(name = "User AI Preference", description = "Kullanıcının AI asistan tercihlerini yönetmesi")
public class UserAiPreferenceController {

    private final UserAiPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Kullanıcının AI tercihlerini getir")
    public ResponseEntity<com.shopai.dto.response.AgentResponses.AiPreferenceResponse> getPreferences(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(preferenceService.getPreferencesResponse(auth.getUserId()));
    }

    @PutMapping
    @Operation(summary = "AI tercihlerini güncelle")
    public ResponseEntity<com.shopai.dto.response.AgentResponses.AiPreferenceResponse> updatePreferences(@AuthenticationPrincipal JwtAuthDetails auth,
                                                            @Valid @RequestBody UpdateAiPreferenceRequest req) {
        return ResponseEntity.ok(preferenceService.updatePreferencesResponse(auth.getUserId(), req));
    }
}
