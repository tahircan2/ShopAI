package com.shopai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.dto.request.AgentRequests.UpdateAiPreferenceRequest;
import com.shopai.entity.User;
import com.shopai.entity.UserAiPreference;
import com.shopai.exception.ResourceNotFoundException;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public UserAiPreference getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    @Transactional
    public UserAiPreference updatePreferences(Long userId, UpdateAiPreferenceRequest req) {
        UserAiPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        if (req.getAutoApproveEnabled() != null) pref.setAutoApproveEnabled(req.getAutoApproveEnabled());
        if (req.getAutoApproveMaxAmount() != null) pref.setAutoApproveMaxAmount(req.getAutoApproveMaxAmount());
        if (req.getUseDefaultAddress() != null) pref.setUseDefaultAddress(req.getUseDefaultAddress());
        if (req.getUseDefaultPayment() != null) pref.setUseDefaultPayment(req.getUseDefaultPayment());
        
        if (req.getAutoApproveCategories() != null) {
            try {
                pref.setAutoApproveCategories(objectMapper.writeValueAsString(req.getAutoApproveCategories()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize categories", e);
            }
        }

        return preferenceRepository.save(pref);
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
                .build();
        
        return preferenceRepository.save(pref);
    }
}
