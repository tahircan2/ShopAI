package com.shopai.service;

import com.shopai.dto.request.AuthRequests.ChangePasswordRequest;
import com.shopai.dto.request.UserRequests.*;
import com.shopai.dto.response.AddressResponse;
import com.shopai.dto.response.AuthResponses.UserInfo;
import com.shopai.entity.Address;
import com.shopai.entity.User;
import com.shopai.entity.WishlistItem;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.AddressRepository;
import com.shopai.repository.ProductRepository;
import com.shopai.repository.UserRepository;
import com.shopai.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final com.shopai.repository.ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ─── Profil ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserInfo getProfile(Long userId) {
        return UserInfo.from(findUser(userId));
    }

    @Transactional
    public UserInfo updateProfile(Long userId, UpdateProfileRequest req, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);
        User oldUser = user.toBuilder().build();
        
        user.setFirstName(req.getFirstName().trim());
        user.setLastName(req.getLastName().trim());
        user.setPhone(req.getPhone());
        
        User saved = userRepository.save(user);
        auditLogService.logEntityAction(userId, "USER_PROFILE_UPDATE", oldUser, saved, "User", userId, request);
        
        return UserInfo.from(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mevcut şifre hatalı");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        
        auditLogService.logWithMap(userId, "USER_PASSWORD_CHANGE", "User", userId, 
                Map.of("password", "********"), Map.of("password", "********"), null, request.getHeader("User-Agent"));
    }

    // ─── Adresler ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest req, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = Address.builder()
                .user(user)
                .label(req.getLabel())
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .district(req.getDistrict())
                .postalCode(req.getPostalCode())
                .country(req.getCountry() != null ? req.getCountry() : "Türkiye")
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        Address saved = addressRepository.save(address);
        auditLogService.logEntityAction(userId, "USER_ADDRESS_ADD", null, saved, "Address", saved.getId(), request);
        
        return AddressResponse.from(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest req, jakarta.servlet.http.HttpServletRequest request) {
        // Ownership check — kullanıcının kendi adresi mi?
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));

        Address oldAddress = address.toBuilder().build();

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setLabel(req.getLabel());
        address.setFullName(req.getFullName());
        address.setPhone(req.getPhone());
        address.setAddressLine1(req.getAddressLine1());
        address.setAddressLine2(req.getAddressLine2());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setPostalCode(req.getPostalCode());
        if (req.getCountry() != null) address.setCountry(req.getCountry());
        if (req.getIsDefault() != null) address.setIsDefault(req.getIsDefault());

        Address saved = addressRepository.save(address);
        auditLogService.logEntityAction(userId, "USER_ADDRESS_UPDATE", oldAddress, saved, "Address", addressId, request);
        
        return AddressResponse.from(saved);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId, jakarta.servlet.http.HttpServletRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));
        
        auditLogService.logEntityAction(userId, "USER_ADDRESS_DELETE", address, null, "Address", addressId, request);
        addressRepository.delete(address);
    }

    // ─── Wishlist ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<com.shopai.dto.response.ProductResponses.ProductSummaryResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(WishlistItem::getProduct)
                .map(com.shopai.dto.response.ProductResponses.ProductSummaryResponse::from)
                .toList();
    }

    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // zaten favoride, idempotent
        }
        User user = findUser(userId);
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün", productId));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();
        wishlistRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserInfo> getAllUsers(int page, int size, String search, String role) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        User.Role userRole = role != null ? User.Role.valueOf(role.toUpperCase()) : null;
        return userRepository.searchUsers(search, userRole, pageable).map(UserInfo::from);
    }

    @Transactional
    public void deleteUser(Long userId, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);
        auditLogService.logEntityAction(null, "USER_DELETE_ADMIN", user, null, "User", userId, request);
        userRepository.delete(user);
    }

    @Transactional
    public UserInfo updateUserRole(Long userId, String role, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);
        User oldUser = user.toBuilder().build();
        user.setRole(User.Role.valueOf(role.toUpperCase()));
        User saved = userRepository.save(user);
        auditLogService.logEntityAction(null, "USER_ROLE_UPDATE", oldUser, saved, "User", userId, request);
        return UserInfo.from(saved);
    }

    @Transactional
    public UserInfo toggleUserActive(Long userId, jakarta.servlet.http.HttpServletRequest request) {
        User user = findUser(userId);
        User oldUser = user.toBuilder().build();
        user.setIsActive(!user.getIsActive());
        User saved = userRepository.save(user);
        auditLogService.logEntityAction(null, "USER_ACTIVE_TOGGLE", oldUser, saved, "User", userId, request);
        return UserInfo.from(saved);
    }

    // ─── Yorumlarım ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.shopai.dto.response.ProductResponses.ReviewResponse> getMyReviews(Long userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 50));
        return reviewRepository.findByUserId(userId, pageable).map(com.shopai.dto.response.ProductResponses.ReviewResponse::from);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", userId));
    }
}
