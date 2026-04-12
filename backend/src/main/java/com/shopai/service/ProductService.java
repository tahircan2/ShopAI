package com.shopai.service;

import com.shopai.dto.request.ProductRequests.*;
import com.shopai.dto.response.ProductResponses.*;
import com.shopai.entity.Category;
import com.shopai.entity.Order;
import com.shopai.entity.Product;
import com.shopai.entity.Review;
import com.shopai.entity.User;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ConflictException;
import com.shopai.exception.ForbiddenException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    // ─── Ürün Listesi (filtreli, sayfalı) ───────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(ProductFilterRequest filter) {
        // Resolve category hierarchy if necessary
        if (filter.getCategorySlug() != null || filter.getCategoryId() != null) {
            Category baseCategory = null;
            if (filter.getCategoryId() != null) {
                baseCategory = categoryRepository.findById(filter.getCategoryId()).orElse(null);
            } else if (filter.getCategorySlug() != null && !filter.getCategorySlug().isBlank()) {
                String slug = filter.getCategorySlug().trim().toLowerCase();
                baseCategory = categoryRepository.findBySlug(slug).orElse(null);
            }

            if (baseCategory != null) {
                java.util.List<Long> allIds = new java.util.ArrayList<>();
                collectCategoryIds(baseCategory, allIds);
                filter.setCategoryIds(allIds);
            }
        }

        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? Math.min(filter.getSize(), 50) : 20
        );
        return productRepository
                .findAll(ProductSpecification.withFilter(filter), pageable)
                .map(ProductSummaryResponse::from);
    }

    // ─── Seller'ın Ürünleri ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getSellerProducts(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductSummaryResponse::from);
    }

    // ─── Ürün Arama ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> search(String q, int page, int size) {
        if (q == null || q.isBlank()) throw new BadRequestException("Arama terimi boş olamaz");
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return productRepository.searchByKeyword(q.trim(), pageable)
                .map(ProductSummaryResponse::from);
    }

    // ─── Ürün Detayı ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findActiveById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + slug));
        return ProductResponse.from(product);
    }

    // ─── Öne Çıkan Ürünler ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getFeatured() {
        Pageable pageable = PageRequest.of(0, 12);
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable)
                .map(ProductSummaryResponse::from)
                .toList();
    }

    // ─── Yorumlar ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long productId, int page, int size) {
        findActiveById(productId); // ürün var mı kontrol
        Pageable pageable = PageRequest.of(page, Math.min(size, 20));
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse addReview(Long productId, Long userId, ReviewRequest req, jakarta.servlet.http.HttpServletRequest request) {
        Product product = findActiveById(productId);

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ConflictException("Bu ürün için zaten bir yorum yazdınız");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", userId));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(req.getRating())
                .title(req.getTitle())
                .comment(req.getComment())
                .isApproved(true)  // Otomatik onayla
                .build();

        reviewRepository.save(review);
        updateProductRating(productId);
        
        auditLogService.logEntityAction(userId, "PRODUCT_REVIEW_ADD", null, review, "Review", review.getId(), request);
        
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long productId, Long reviewId, Long userId, boolean isAdmin, jakarta.servlet.http.HttpServletRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı"));

        if (!review.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Bu yorum bu ürüne ait değil");
        }

        // Sadece kendi yorumu ya da admin silebilir
        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bu yorumu silme yetkiniz yok");
        }

        auditLogService.logEntityAction(userId, "PRODUCT_REVIEW_DELETE", review, null, "Review", reviewId, request);
        
        reviewRepository.delete(review);
        updateProductRating(productId);
    }

    // ─── Admin: Ürün CRUD ────────────────────────────────────────────────────
    @Transactional
    public ProductResponse createProduct(CreateProductRequest req, Long sellerId, jakarta.servlet.http.HttpServletRequest request) {
        if (req.getSku() != null && productRepository.findAll().stream()
                .anyMatch(p -> req.getSku().equals(p.getSku()))) {
            throw new ConflictException("Bu SKU zaten kullanılıyor: " + req.getSku());
        }

        User seller = sellerId != null ? userRepository.findById(sellerId).orElse(null) : null;

        Product product = Product.builder()
                .name(req.getName())
                .slug(req.getSlug() != null ? req.getSlug() : toSlug(req.getName()))
                .description(req.getDescription())
                .longDescription(req.getLongDescription())
                .price(req.getPrice())
                .discountedPrice(req.getDiscountedPrice())
                .stockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0)
                .sku(req.getSku())
                .brand(req.getBrand())
                .isFeatured(req.getIsFeatured() != null && req.getIsFeatured())
                .tags(req.getTags())
                .metaTitle(req.getMetaTitle())
                .metaDescription(req.getMetaDescription())
                .seller(seller)
                .build();

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori", req.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        auditLogService.logEntityAction(sellerId, "PRODUCT_CREATE", null, saved, "Product", saved.getId(), request);
        
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest req, Long userId, boolean isAdmin, jakarta.servlet.http.HttpServletRequest request) {
        Product product = findActiveById(id);

        if (!isAdmin) {
            if (product.getSeller() == null || !product.getSeller().getId().equals(userId)) {
                throw new ForbiddenException("Bu ürünü güncellemeye yetkiniz yok");
            }
        }

        // Clone for audit log before changes
        Product oldProduct = product.toBuilder().build();

        product.setName(req.getName());
        if (req.getSlug() != null) product.setSlug(req.getSlug());
        product.setDescription(req.getDescription());
        product.setLongDescription(req.getLongDescription());
        product.setPrice(req.getPrice());
        product.setDiscountedPrice(req.getDiscountedPrice());
        if (req.getStockQuantity() != null) product.setStockQuantity(req.getStockQuantity());
        product.setBrand(req.getBrand());
        if (req.getIsFeatured() != null) product.setIsFeatured(req.getIsFeatured());
        product.setTags(req.getTags());
        product.setMetaTitle(req.getMetaTitle());
        product.setMetaDescription(req.getMetaDescription());

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori", req.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        auditLogService.logEntityAction(userId, "PRODUCT_UPDATE", oldProduct, saved, "Product", id, request);
        
        return ProductResponse.from(saved);
    }

    @Transactional
    public void deleteProduct(Long id, Long userId, boolean isAdmin, jakarta.servlet.http.HttpServletRequest request) {
        Product product = findActiveById(id);

        if (!isAdmin) {
            if (product.getSeller() == null || !product.getSeller().getId().equals(userId)) {
                throw new ForbiddenException("Bu ürünü silmeye yetkiniz yok");
            }
        }

        auditLogService.logEntityAction(userId, "PRODUCT_DELETE", product, null, "Product", id, request);
        
        product.setIsActive(false); // soft delete
        productRepository.save(product);
    }

    // ─── Stats ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(getStartOfCurrentMonth());

        return Map.of(
                "totalProducts", totalProducts,
                "totalOrders", totalOrders,
                "totalUsers", totalUsers,
                "totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                "pendingOrders", pendingOrders,
                "newUsersThisMonth", newUsersThisMonth
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSellerStats(Long sellerId) {
        long totalProducts = productRepository.countBySellerId(sellerId);
        BigDecimal totalRevenue = orderRepository.sumRevenueForSeller(sellerId);
        long totalOrders = orderRepository.countOrdersForSeller(sellerId);
        long pendingOrders = orderRepository.countPendingOrdersForSeller(sellerId);
        BigDecimal monthlyRevenue = orderRepository.sumRevenueForSellerAfter(sellerId, getStartOfCurrentMonth());

        // Average rating for seller's products
        Double avgRating = productRepository.avgRatingForSeller(sellerId);

        return Map.of(
                "totalProducts", totalProducts,
                "totalOrders", totalOrders,
                "totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                "pendingOrders", pendingOrders,
                "avgRating", avgRating != null ? avgRating : 0.0,
                "monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private LocalDateTime getStartOfCurrentMonth() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    public Product findActiveById(Long id) {
        return productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün", id));
    }

    private void updateProductRating(Long productId) {
        Double avg = reviewRepository.findAvgRatingByProductId(productId);
        Long count = reviewRepository.countByProductId(productId);
        BigDecimal avgDecimal = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        productRepository.updateRating(productId, avgDecimal, count != null ? count.intValue() : 0);
    }

    private String toSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return normalized + "-" + System.currentTimeMillis();
    }

    private void collectCategoryIds(Category category, java.util.List<Long> ids) {
        if (category == null) return;
        
        ids.add(category.getId());
        
        // Use a more defensive check for children to avoid lazy loading issues if possible
        List<Category> children = category.getChildren();
        if (children != null && !children.isEmpty()) {
            for (Category child : children) {
                if (child != null && Boolean.TRUE.equals(child.getIsActive())) {
                    collectCategoryIds(child, ids);
                }
            }
        }
    }
}
