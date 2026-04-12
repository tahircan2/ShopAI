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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper auditMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        
        // Audit logları için güvenli ve döngüsüz serileştirme yapacak özel mapper
        this.auditMapper = objectMapper.copy();
        
        // Hibernate modülü: Lazy loading olan alanların (proxy) serileştirmeye çalışılmasını ve hata vermesini engeller
        this.auditMapper.registerModule(new com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module());
        
        // Güvenlik: Hassas verileri filtreleyen Mixin'ler
        this.auditMapper.addMixIn(com.shopai.entity.User.class, com.shopai.security.AuditMixins.UserMixin.class);
        this.auditMapper.addMixIn(com.shopai.entity.RefreshToken.class, com.shopai.security.AuditMixins.RefreshTokenMixin.class);
        
        // Diğer iyileştirmeler
        this.auditMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType) {
        this.log(userId, action, entityType, null, "SYSTEM", "INTERNAL", null, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType, Long entityId) {
        this.log(userId, action, entityType, entityId, "SYSTEM", "INTERNAL", null, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType, Long entityId,
            String ipAddress, String userAgent, String oldData, String newData) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .oldData(oldData)
                    .newData(newData)
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("[AuditLog] Kayıt başarısız — action: {}, userId: {}", action, userId, e);
        }
    }

    public void logWithRequest(Long userId, String action, String entityType, Long entityId,
                               String oldData, String newData, jakarta.servlet.http.HttpServletRequest request) {
        String ip = "SYSTEM";
        String ua = "SYSTEM_SCHEDULER";
        
        if (request != null) {
            ip = getClientIp(request);
            ua = request.getHeader("User-Agent");
        }
        
        this.log(userId, action, entityType, entityId, ip, ua, oldData, newData);
    }

    public void logWithMap(Long userId, String action, String entityType, Long entityId,
                           Map<String, ?> oldMap, Map<String, ?> newMap, String ip, String ua) {
        try {
            String oldStr = oldMap != null ? auditMapper.writeValueAsString(oldMap) : null;
            String newStr = newMap != null ? auditMapper.writeValueAsString(newMap) : null;
            this.log(userId, action, entityType, entityId, ip, ua, oldStr, newStr);
        } catch (JsonProcessingException e) {
            log.error("[AuditLog] JSON serialization failed for action: {}", action, e);
        }
    }

    public void logEntityAction(Long userId, String action, Object oldState, Object newState, 
                                 String entityType, Long entityId, jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Hassas verileri filtreleyen auditMapper kullanılıyor
            String oldStr = oldState != null ? auditMapper.writeValueAsString(oldState) : null;
            String newStr = newState != null ? auditMapper.writeValueAsString(newState) : null;
            logWithRequest(userId, action, entityType, entityId, oldStr, newStr, request);
        } catch (JsonProcessingException e) {
            log.error("[AuditLog] JSON serialization failed for action: {}", action, e);
        }
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLatestLogs(int limit) {
        return auditLogRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getLatestLogsPaginated(int page, int size) {
        return auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public static final class Actions {
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
        public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";
        public static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";
        public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
        public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";
        public static final String ORDER_CREATED = "ORDER_CREATED";
        public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
        public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
        public static final String CART_ITEM_ADDED = "CART_ITEM_ADDED";
        public static final String COUPON_APPLIED = "COUPON_APPLIED";
        public static final String INJECTION_DETECTED = "INJECTION_DETECTED";
        public static final String TOKEN_CLEANUP = "TOKEN_CLEANUP";
        public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
        public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
        public static final String PRODUCT_DELETED = "PRODUCT_DELETED";
        public static final String ADMIN_ACCESS = "ADMIN_ACCESS";
    }
}
