package com.shopai.service;

import com.shopai.dto.response.ProductResponses.CategoryResponse;
import com.shopai.entity.Category;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllWithChildren() {
        return categoryRepository.findRootCategoriesWithChildren()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + slug));
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse createCategory(com.shopai.dto.request.ProductRequests.CategoryRequest req, jakarta.servlet.http.HttpServletRequest request) {
        if (categoryRepository.findBySlug(req.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Bu slug zaten kullanılıyor");
        }

        Category parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent kategori", req.getParentId()));
        }

        Category category = Category.builder()
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .parent(parent)
                .imageUrl(req.getImageUrl())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();

        Category saved = categoryRepository.save(category);
        auditLogService.logEntityAction(null, "CATEGORY_CREATE", null, saved, "Category", saved.getId(), request);
        
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, com.shopai.dto.request.ProductRequests.CategoryRequest req, jakarta.servlet.http.HttpServletRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));

        if (!category.getSlug().equals(req.getSlug()) && categoryRepository.findBySlug(req.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Bu slug zaten kullanılıyor");
        }

        Category oldCategory = category.toBuilder().build();

        Category parent = null;
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new IllegalArgumentException("Bir kategori kendisinin üst kategorisi olamaz");
            }
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent kategori", req.getParentId()));
        }

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setDescription(req.getDescription());
        category.setParent(parent);
        category.setImageUrl(req.getImageUrl());
        category.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        if (req.getSortOrder() != null) category.setSortOrder(req.getSortOrder());

        Category saved = categoryRepository.save(category);
        auditLogService.logEntityAction(null, "CATEGORY_UPDATE", oldCategory, saved, "Category", id, request);
        
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deleteCategory(Long id, jakarta.servlet.http.HttpServletRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        if (!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("Alt kategorisi olan bir kategori silinemez");
        }
        
        auditLogService.logEntityAction(null, "CATEGORY_DELETE", category, null, "Category", id, request);
        categoryRepository.delete(category);
    }
}
