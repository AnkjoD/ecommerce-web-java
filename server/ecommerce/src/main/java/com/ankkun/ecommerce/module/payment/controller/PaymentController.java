package com.ankkun.ecommerce.module.payment.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-url")
    public ResponseEntity<ApiResponse<?>> createUrl(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String orderId = body.get("order_id");
        return ResponseEntity.ok(ApiResponse.ok(paymentService.createPaymentUrl(orderId, ip)));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<?>> vnpayReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.vnpayReturn(params)));
    }
}
