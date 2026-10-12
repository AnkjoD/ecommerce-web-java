package com.ankkun.ecommerce.module.order.controller;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders") @RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService service;
    public record StatusRequest(OrderStatus status) {}
    @GetMapping public Object list(@RequestParam(required=false) String status,@RequestParam(required=false) String search,@RequestParam(required=false) String userId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit) {
        return ApiResponse.ok(service.adminHistory(status,search,userId,page,limit));
    }
    public record PaymentRequest(com.ankkun.ecommerce.common.enums.PaymentStatus status) {}
    @PatchMapping("/{id}/payment-status")public Object payment(@PathVariable String id,@RequestBody PaymentRequest request){return ApiResponse.ok(service.updatePayment(id,request.status()));}
    @PatchMapping("/{id}/status") public Object update(@PathVariable String id,@RequestBody StatusRequest request) {
        return ApiResponse.ok(service.advance(id,request.status()));
    }
}
