package com.ankkun.ecommerce.module.product.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.ok(productService.findAll(params)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<?>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.findBySlug(slug)));
    }
}
