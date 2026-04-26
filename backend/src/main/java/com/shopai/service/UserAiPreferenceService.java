package com.shopai.service;

import com.shopai.dto.request.AgentRequests.UpdateAiPreferenceRequest;
import com.shopai.entity.User;
import com.shopai.entity.UserAiPreference;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.dto.response.AgentResponses.AiPreferenceResponse;
import com.shopai.repository.UserAiPreferenceRepository;
import com.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAiPreferenceService {

    private final UserAiPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserAiPreference getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    @Transactional
    public UserAiPreference updatePreferences(Long userId, UpdateAiPreferenceRequest req) {
        UserAiPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        if (req.getAutoApproveEnabled() != null)
            pref.setAutoApproveEnabled(req.getAutoApproveEnabled());
        if (req.getAutoApproveMaxAmount() != null)
            pref.setAutoApproveMaxAmount(req.getAutoApproveMaxAmount());
        if (req.getUseDefaultAddress() != null)
            pref.setUseDefaultAddress(req.getUseDefaultAddress());
        if (req.getUseDefaultPayment() != null)
            pref.setUseDefaultPayment(req.getUseDefaultPayment());

        if (req.getAutoApproveCategories() != null) {
            pref.setAutoApproveCategories(req.getAutoApproveCategories());
        }

        return preferenceRepository.save(pref);
    }

    @Transactional(readOnly = true)
    public AiPreferenceResponse getPreferencesResponse(Long userId) {
        UserAiPreference pref = getPreferences(userId);
        return mapToResponse(pref);
    }

    @Transactional
    public AiPreferenceResponse updatePreferencesResponse(Long userId, UpdateAiPreferenceRequest req) {
        UserAiPreference pref = updatePreferences(userId, req);
        return mapToResponse(pref);
    }

    private AiPreferenceResponse mapToResponse(UserAiPreference pref) {
        return AiPreferenceResponse.builder()
                .autoApproveEnabled(pref.getAutoApproveEnabled())
                .autoApproveMaxAmount(pref.getAutoApproveMaxAmount())
                .autoApproveCategories(pref.getAutoApproveCategories() != null ? pref.getAutoApproveCategories()
                        : java.util.Collections.emptyList())
                .useDefaultAddress(pref.getUseDefaultAddress())
                .useDefaultPayment(pref.getUseDefaultPayment())
                .dailyTransactionLimit(pref.getDailyTransactionLimit())
                .maxOrderAmount(pref.getMaxOrderAmount())
                .todayTransactionCount(0) // Basit tutmak için 0 döner, gerekirse eklenebilir
                .build();
    }

    @Transactional
    public UserAiPreference createDefaultPreference(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAiPreference pref = UserAiPreference.builder()
                .user(user)
                .autoApproveEnabled(false)
                .useDefaultAddress(true)
                .useDefaultPayment(true)
                .dailyTransactionLimit(10)
                .maxOrderAmount(new java.math.BigDecimal("5000.00"))
                .build();

        return preferenceRepository.save(pref);
    }
}
