package com.shopai.controller;

import com.shopai.dto.response.AuthResponses.UserInfo;
import com.shopai.entity.User;
import com.shopai.entity.AuditLog;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.service.AuditLogService;
import com.shopai.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin panel işlemleri")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final com.shopai.service.CategoryService categoryService;

    @GetMapping("/audit-logs")
    @Operation(summary = "Sistem loglarını getir (Admin)")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getLatestLogsPaginated(page, size));
    }

    @GetMapping("/users")
    @Operation(summary = "Tüm kullanıcılar (Admin)")
    public ResponseEntity<Page<UserInfo>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User.Role roleEnum = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleEnum = User.Role.valueOf(role);
            } catch (Exception e) {
            }
        }
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search : null;

        return ResponseEntity.ok(
                userRepository.searchUsers(searchTerm, roleEnum,
                        PageRequest.of(page, Math.min(size, 100),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(UserInfo::from));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Kullanıcı Sil (Admin)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        
        auditLogService.logEntityAction(null, "ADMIN_USER_DELETE", user, null, "User", id, request);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Kullanıcı detayı (Admin)")
    public ResponseEntity<UserInfo> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        return ResponseEntity.ok(UserInfo.from(user));
    }

    @PutMapping("/users/{id}/activate")
    @Operation(summary = "Kullanıcıyı aktifleştir (Admin)")
    public ResponseEntity<UserInfo> activateUser(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        
        String oldState = user.getIsActive() ? "ACTIVE" : "INACTIVE";
        user.setIsActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        
        auditLogService.logWithRequest(null, "ADMIN_USER_ACTIVATE", "User", id, oldState, "ACTIVE", request);
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/deactivate")
    @Operation(summary = "Kullanıcıyı devre dışı bırak (Admin)")
    public ResponseEntity<UserInfo> deactivateUser(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        
        String oldState = user.getIsActive() ? "ACTIVE" : "INACTIVE";
        user.setIsActive(false);
        
        auditLogService.logWithRequest(null, "ADMIN_USER_DEACTIVATE", "User", id, oldState, "INACTIVE", request);
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/toggle-active")
    @Operation(summary = "Kullanıcı durumunu değiştir (Admin)")
    public ResponseEntity<UserInfo> toggleActive(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        
        boolean oldVal = user.getIsActive();
        user.setIsActive(!oldVal);
        if (user.getIsActive()) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        
        auditLogService.logWithRequest(null, "ADMIN_USER_TOGGLE", "User", id, 
                String.valueOf(oldVal), String.valueOf(!oldVal), request);
                
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Kullanıcı rolünü güncelle (Admin)")
    public ResponseEntity<UserInfo> updateUserRole(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        
        String oldRole = user.getRole().name();
        String role = body.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            User.Role newRole = User.Role.valueOf(role.toUpperCase().trim());
            user.setRole(newRole);
            auditLogService.logWithRequest(null, "ADMIN_ROLE_CHANGE", "User", id, oldRole, newRole.name(), request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    // --- Kategoriler ---

    @PostMapping("/categories")
    @Operation(summary = "Kategori ekle (Admin)")
    public ResponseEntity<com.shopai.dto.response.ProductResponses.CategoryResponse> createCategory(
            @RequestBody com.shopai.dto.request.ProductRequests.CategoryRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(categoryService.createCategory(req, request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Kategori güncelle (Admin)")
    public ResponseEntity<com.shopai.dto.response.ProductResponses.CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody com.shopai.dto.request.ProductRequests.CategoryRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, req, request));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Kategori sil (Admin)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        categoryService.deleteCategory(id, request);
        return ResponseEntity.noContent().build();
    }
}
