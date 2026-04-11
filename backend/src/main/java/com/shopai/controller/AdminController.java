package com.shopai.controller;

import com.shopai.dto.response.AuthResponses.UserInfo;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
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

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin panel işlemleri")
public class AdminController {

    private final UserRepository userRepository;

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
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kullanıcı", id);
        }
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
    public ResponseEntity<UserInfo> activateUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        user.setIsActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/deactivate")
    @Operation(summary = "Kullanıcıyı devre dışı bırak (Admin)")
    public ResponseEntity<UserInfo> deactivateUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        user.setIsActive(false);
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/toggle-active")
    @Operation(summary = "Kullanıcı durumunu değiştir (Admin)")
    public ResponseEntity<UserInfo> toggleActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        user.setIsActive(!user.getIsActive());
        if (user.getIsActive()) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Kullanıcı rolünü güncelle (Admin)")
    public ResponseEntity<UserInfo> updateUserRole(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        String role = body.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            user.setRole(User.Role.valueOf(role.toUpperCase().trim()));
        } catch (IllegalArgumentException e) {
            // Invalid role string — 400 instead of 500
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(UserInfo.from(userRepository.save(user)));
    }
}
