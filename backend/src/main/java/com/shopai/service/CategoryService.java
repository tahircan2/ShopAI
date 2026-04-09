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
}
