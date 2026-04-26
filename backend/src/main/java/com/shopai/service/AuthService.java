package com.shopai.service;

import com.shopai.dto.request.AuthRequests.*;
import com.shopai.dto.response.AuthResponses.*;
import com.shopai.entity.RefreshToken;
import com.shopai.entity.User;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ConflictException;
import com.shopai.exception.InvalidTokenException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.RefreshTokenRepository;
import com.shopai.repository.UserRepository;
import com.shopai.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CookieService cookieService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final BlacklistService blacklistService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes}")
    private int lockoutDurationMinutes;

    @Value("${app.security.password-reset-expiry-hours}")
    private int passwordResetExpiryHours;

    @Value("${app.security.email-verify-expiry-hours}")
    private int emailVerifyExpiryHours;

    // ────────────────────────────────────────────────
    // REGISTER
    // ────────────────────────────────────────────────
    @Transactional
    public MessageResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ConflictException("Bu e-posta adresi zaten kayıtlı");
        }

        String verifyToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .phone(req.getPhone())
                .shopName(req.getShopName() != null ? req.getShopName().trim() : null)
                .shopDescription(req.getShopDescription() != null ? req.getShopDescription().trim() : null)
                .emailVerifyToken(verifyToken)
                .build();

        userRepository.save(user);
        emailService.sendEmailVerification(user.getEmail(), verifyToken);

        log.info("New user registered: {}", user.getEmail());
        return new MessageResponse("Kayıt başarılı. Lütfen e-postanızı doğrulayın.");
    }

    // ────────────────────────────────────────────────
    // LOGIN
    // ────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String email = req.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("E-posta veya şifre hatalı"));

        // Hesap kilitli mi?
        if (user.isLocked()) {
            throw new BadRequestException("Hesabınız geçici olarak kilitlendi. Lütfen daha sonra tekrar deneyin.");
        }

        // Hesap aktif mi?
        if (!user.getIsActive()) {
            throw new BadRequestException("Hesabınız devre dışı bırakılmış.");
        }

        // E-posta doğrulandı mı?
        if (!user.getIsEmailVerified()) {
            throw new BadRequestException("Lütfen önce e-posta adresinizi doğrulayın.");
        }

        // Şifre kontrolü
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadRequestException("E-posta veya şifre hatalı");
        }

        // Başarılı giriş — failed attempts sıfırla
        userRepository.resetFailedAttempts(user.getId());
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        // Token üret + cookie set et
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = generateAndSaveRefreshToken(user, request);

        cookieService.setAccessTokenCookie(response, accessToken);
        cookieService.setRefreshTokenCookie(response, refreshToken);

        auditLogService.logWithRequest(user.getId(), "USER_LOGIN", "User", user.getId(), null, null, request);
        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .expiresIn(jwtUtil.getAccessExpiration() / 1000)
                .user(UserInfo.from(user))
                .build();
    }

    // ────────────────────────────────────────────────
    // REFRESH TOKEN
    // ────────────────────────────────────────────────
    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieService.extractRefreshTokenFromCookie(request);

        if (rawRefreshToken == null) {
            throw new InvalidTokenException("Refresh token bulunamadı");
        }

        String tokenHash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Geçersiz refresh token"));

        if (!stored.isValid()) {
            // Token çalınmış veya süresi dolmuş — tüm tokenları iptal et
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), LocalDateTime.now());
            cookieService.clearAuthCookies(response);
            auditLogService.logWithRequest(stored.getUser().getId(), "SUSPICIOUS_REFRESH_ATTEMPT", "Token", null, null, null, request);
            throw new InvalidTokenException("Oturum geçersiz. Lütfen tekrar giriş yapın.");
        }

        // Token Rotation — eski token'ı iptal et, yeni üret
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = generateAndSaveRefreshToken(user, request);

        cookieService.setAccessTokenCookie(response, newAccessToken);
        cookieService.setRefreshTokenCookie(response, newRefreshToken);

        return AuthResponse.builder()
                .expiresIn(jwtUtil.getAccessExpiration() / 1000)
                .user(UserInfo.from(user))
                .build();
    }

    // ────────────────────────────────────────────────
    // LOGOUT
    // ────────────────────────────────────────────────
    @Transactional
    public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) {
        // Token'ları kara listeye al
        String accessToken = cookieService.extractTokenFromCookie(request, "access_token");
        String refreshToken = cookieService.extractRefreshTokenFromCookie(request);

        if (accessToken != null) blacklistService.blacklistToken(accessToken);
        if (refreshToken != null) blacklistService.blacklistToken(refreshToken);

        if (userId != null) {
            refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
        }
        cookieService.clearAuthCookies(response);
        // Modern tarayıcılar için kesin çözüm: Tüm session verilerini temizle komutu gönder
        response.setHeader("Clear-Site-Data", "\"cookies\", \"storage\"");

        if (userId != null) {
            auditLogService.logWithRequest(userId, "USER_LOGOUT", "User", userId, null, null, request);
            log.info("USER_LOGOUT: Successfully logged out userId: {}", userId);
        } else {
            log.info("ANONYMOUS_LOGOUT: Clearing cookies for unauthorized user");
        }
    }

    // ────────────────────────────────────────────────
    // SESSION CHECK (/me)
    // ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public SessionStatus checkSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        return SessionStatus.builder()
                .isLoggedIn(true)
                .user(UserInfo.from(user))
                .build();
    }

    // ────────────────────────────────────────────────
    // FORGOT PASSWORD
    // ────────────────────────────────────────────────
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail().toLowerCase()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
        // Token'lı SHA-256 hash olarak sakla — plaintext asla saklanmaz
        String tokenHash = sha256(rawToken);
        user.setPasswordResetTokenHash(tokenHash);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(passwordResetExpiryHours));
        userRepository.save(user);
        emailService.sendPasswordReset(user.getEmail(), rawToken);
    });
    // Kullanıcı bulunsun veya bulunmasın aynı mesajı dön — enumeration saldırısını önler
    return new MessageResponse("Şifre sıfırlama linki e-postanıza gönderildi.");
    }

    // ────────────────────────────────────────────────
    // RESET PASSWORD
    // ────────────────────────────────────────────────
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        // Token SHA-256 hash olarak saklanır — tek sorguda bulunur, tüm tablo taranır
        String tokenHash = sha256(req.getToken());
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .filter(u -> u.getPasswordResetExpires() != null
                        && LocalDateTime.now().isBefore(u.getPasswordResetExpires()))
                .orElseThrow(() -> new InvalidTokenException("Geçersiz veya süresi dolmuş sıfırlama tokeni"));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        // Tüm aktif oturumları kapat
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        auditLogService.log(user.getId(), "PASSWORD_RESET", null);

        return new MessageResponse("Şifreniz başarıyla sıfırlandı. Lütfen giriş yapın.");
    }

    // ────────────────────────────────────────────────
    // VERIFY EMAIL
    // ────────────────────────────────────────────────
    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() -> new InvalidTokenException("Geçersiz doğrulama tokeni"));

        user.setIsEmailVerified(true);
        user.setEmailVerifyToken(null);
        userRepository.save(user);

        auditLogService.log(user.getId(), "EMAIL_VERIFIED", null);
        return new MessageResponse("E-posta adresiniz başarıyla doğrulandı.");
    }

    // ────────────────────────────────────────────────
    // RESEND VERIFICATION
    // ────────────────────────────────────────────────
    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest req) {
        userRepository.findByEmail(req.getEmail().toLowerCase()).ifPresent(user -> {
            if (!user.getIsEmailVerified()) {
                String newToken = UUID.randomUUID().toString();
                user.setEmailVerifyToken(newToken);
                userRepository.save(user);
                emailService.sendEmailVerification(user.getEmail(), newToken);
            }
        });
        return new MessageResponse("Doğrulama e-postası gönderildi.");
    }

    // ────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("Account locked due to failed attempts: {}", user.getEmail());
            auditLogService.log(user.getId(), "ACCOUNT_LOCKED", "User", user.getId(), null, null, null, null);
        }
        userRepository.save(user);
    }

    private String generateAndSaveRefreshToken(User user, HttpServletRequest request) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceInfo(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration() / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
