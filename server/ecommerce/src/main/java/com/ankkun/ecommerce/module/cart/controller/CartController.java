package com.ankkun.ecommerce.module.cart.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.cart.dto.CartItemRequest;
import com.ankkun.ecommerce.module.cart.dto.CartItemUpdateRequest;
import com.ankkun.ecommerce.module.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/api/cart")
    public ResponseEntity<ApiResponse<?>> getCart(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping("/api/cart/items")
    public ResponseEntity<ApiResponse<?>> addItem(@AuthenticationPrincipal String userId, @Valid @RequestBody CartItemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.addItem(userId, req)));
    }

    @PutMapping("/api/cart/items/{variantId}")
    public ResponseEntity<ApiResponse<?>> updateItem(@AuthenticationPrincipal String userId,
                                                     @PathVariable String variantId,
                                                     @RequestBody CartItemUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItem(userId, variantId, req.getQuantity())));
    }

    @DeleteMapping("/api/cart/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@AuthenticationPrincipal String userId, @PathVariable String variantId) {
        cartService.removeItem(userId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Removed", null));
    }
}
