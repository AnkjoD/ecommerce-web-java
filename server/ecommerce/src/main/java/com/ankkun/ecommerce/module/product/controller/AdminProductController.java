package com.ankkun.ecommerce.module.product.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.product.dto.ProductCreateDto;
import com.ankkun.ecommerce.module.product.dto.ProductUpdateDto;
import com.ankkun.ecommerce.module.product.dto.VariantCreateDto;
import com.ankkun.ecommerce.module.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody ProductCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(productService.create(dto)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id, @RequestBody ProductUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm", null));
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<?>> addVariant(@PathVariable String id, @Valid @RequestBody VariantCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(productService.addVariant(id, dto)));
    }
}
