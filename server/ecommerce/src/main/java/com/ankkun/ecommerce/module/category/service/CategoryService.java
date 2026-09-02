package com.ankkun.ecommerce.module.category.service;

import com.ankkun.ecommerce.common.exception.ConflictException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.category.dto.CategoryCreateDto;
import com.ankkun.ecommerce.module.category.dto.CategoryResponse;
import com.ankkun.ecommerce.module.category.dto.CategoryUpdateDto;
import com.ankkun.ecommerce.module.category.entity.Category;
import com.ankkun.ecommerce.module.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findByParentIdIsNullOrderBySortOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CategoryResponse findBySlug(String slug) {
        return toResponse(categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Category not found")));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateDto dto) {
        if (categoryRepository.existsBySlug(dto.getSlug())) throw new ConflictException("Slug đã tồn tại");

        Category cat = Category.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .parentId(dto.getParentId())
                .imageUrl(dto.getImageUrl())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(true)
                .build();
        return toResponse(categoryRepository.save(cat));
    }

    @Transactional
    public CategoryResponse update(String id, CategoryUpdateDto dto) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (dto.getName() != null) cat.setName(dto.getName());
        if (dto.getImageUrl() != null) cat.setImageUrl(dto.getImageUrl());
        if (dto.getSortOrder() != null) cat.setSortOrder(dto.getSortOrder());
        if (dto.getActive() != null) cat.setIsActive(dto.getActive());
        return toResponse(categoryRepository.save(cat));
    }

    @Transactional
    public void delete(String id) {
        categoryRepository.deleteById(id);
    }

    // Recursive: collect all descendant IDs
    public List<String> getDescendantIds(String parentId) {
        List<String> result = new ArrayList<>();
        List<Category> children = categoryRepository.findByParentId(parentId);
        for (Category child : children) {
            result.add(child.getId());
            result.addAll(getDescendantIds(child.getId()));
        }
        return result;
    }

    private CategoryResponse toResponse(Category cat) {
        CategoryResponse r = new CategoryResponse();
        r.setId(cat.getId());
        r.setName(cat.getName());
        r.setSlug(cat.getSlug());
        r.setParentId(cat.getParentId());
        r.setImageUrl(cat.getImageUrl());
        r.setSortOrder(cat.getSortOrder());
        r.setActive(cat.getIsActive());
        if (cat.getChildren() != null) {
            r.setChildren(cat.getChildren().stream().map(this::toResponse).collect(Collectors.toList()));
        }
        return r;
    }
}
