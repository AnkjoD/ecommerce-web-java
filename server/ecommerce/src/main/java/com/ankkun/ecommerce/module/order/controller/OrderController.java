package com.ankkun.ecommerce.module.order.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.order.dto.OrderCreateRequest;
import com.ankkun.ecommerce.module.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createFromCart(@AuthenticationPrincipal String userId, @Valid @RequestBody OrderCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.createFromCart(userId, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getOrder(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrder(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelOrder(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(id, userId)));
    }
}
