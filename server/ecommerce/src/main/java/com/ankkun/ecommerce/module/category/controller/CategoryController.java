package com.ankkun.ecommerce.module.category.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.category.dto.CategoryCreateDto;
import com.ankkun.ecommerce.module.category.dto.CategoryUpdateDto;
import com.ankkun.ecommerce.module.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping({"/api/categories", "/api/categories/tree"})
    public ResponseEntity<ApiResponse<?>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findAll()));
    }

    @GetMapping("/api/categories/{slug}")
    public ResponseEntity<ApiResponse<?>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findBySlug(slug)));
    }

    @GetMapping("/api/admin/categories") public Object adminList(){return ApiResponse.ok(categoryService.adminList());}

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CategoryCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.create(dto)));
    }

    @PatchMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id, @Valid @RequestBody CategoryUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, dto)));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa danh mục", null));
    }
}
