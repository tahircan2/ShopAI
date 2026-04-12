package com.shopai.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Audit logları için hassas verilerin JSON serileştirmesinden muaf tutulmasını sağlayan Mixin tanımları.
 * Bu sayede Entity sınıflarını kirletmeden, sadece logging sırasında filtreleme yapılır.
 */
public class AuditMixins {

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public abstract static class UserMixin {
        @JsonIgnore
        private String passwordHash;
        
        @JsonIgnore
        private String emailVerifyToken;
        
        @JsonIgnore
        private String passwordResetTokenHash;
        
        @JsonIgnore
        private String passwordResetExpires;
    }

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public abstract static class RefreshTokenMixin {
        @JsonIgnore
        private String tokenHash;
    }

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public abstract static class GenericMixin { }
}
