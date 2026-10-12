package com.ankkun.ecommerce.module.coupon.controller;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.coupon.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService service;
    public record ValidateRequest(@NotBlank String code, @NotNull @DecimalMin("0") BigDecimal order_total) {}
    @GetMapping("/active") public Object active() { return ApiResponse.ok(service.active()); }
    @PostMapping("/validate") public Object validate(@Valid @RequestBody ValidateRequest req) {
        var coupon = service.validate(req.code());
        return Map.of("valid", true, "coupon_id", coupon.getId(), "discount_amount", service.discount(coupon, req.order_total()), "coupon", coupon);
    }
}
