package com.shopai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.entity.AuditLog;
import com.shopai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType, Long entityId,
                    String ipAddress, String userAgent) {
        try {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("[AuditLog] Kayıt başarısız — action: {}, userId: {}", action, userId, e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String ipAddress) {
        log(userId, action, null, null, ipAddress, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWithData(Long userId, String action, String entityType, Long entityId,
                            String oldData, String newData, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldData(oldData)
                    .newData(newData)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[AuditLog] Kayıt başarısız — action: {}, userId: {}", action, userId, e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWithMap(Long userId, String action, String entityType, Long entityId,
                           Map<String, Object> oldDataMap, Map<String, Object> newDataMap,
                           String ipAddress, String userAgent) {
        try {
            String oldDataStr = oldDataMap != null ? objectMapper.writeValueAsString(oldDataMap) : null;
            String newDataStr = newDataMap != null ? objectMapper.writeValueAsString(newDataMap) : null;
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldData(oldDataStr)
                    .newData(newDataStr)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.error("[AuditLog] Serialization hatası — action: {}", action, e);
        } catch (Exception e) {
            log.error("[AuditLog] Kayıt başarısız — action: {}, userId: {}", action, userId, e);
        }
    }

    public static final class Actions {
        public static final String USER_LOGIN             = "USER_LOGIN";
        public static final String USER_LOGOUT            = "USER_LOGOUT";
        public static final String USER_REGISTER          = "USER_REGISTER";
        public static final String LOGIN_FAILED           = "LOGIN_FAILED";
        public static final String ACCOUNT_LOCKED         = "ACCOUNT_LOCKED";
        public static final String PASSWORD_CHANGED       = "PASSWORD_CHANGED";
        public static final String PASSWORD_RESET         = "PASSWORD_RESET";
        public static final String EMAIL_VERIFIED         = "EMAIL_VERIFIED";
        public static final String SESSION_EXPIRED        = "SESSION_EXPIRED";
        public static final String TOKEN_REFRESHED        = "TOKEN_REFRESHED";
        public static final String ORDER_CREATED          = "ORDER_CREATED";
        public static final String ORDER_CANCELLED        = "ORDER_CANCELLED";
        public static final String ORDER_STATUS_CHANGED   = "ORDER_STATUS_CHANGED";
        public static final String CART_ITEM_ADDED        = "CART_ITEM_ADDED";
        public static final String COUPON_APPLIED         = "COUPON_APPLIED";
        public static final String INJECTION_DETECTED     = "INJECTION_DETECTED";
        public static final String TOKEN_CLEANUP          = "TOKEN_CLEANUP";
        public static final String PRODUCT_CREATED        = "PRODUCT_CREATED";
        public static final String PRODUCT_UPDATED        = "PRODUCT_UPDATED";
        public static final String PRODUCT_DELETED        = "PRODUCT_DELETED";
        public static final String ADMIN_ACCESS           = "ADMIN_ACCESS";
    }
}
