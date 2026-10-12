package com.ankkun.ecommerce.module.coupon.controller;
import com.ankkun.ecommerce.common.dto.*;
import com.ankkun.ecommerce.common.exception.*;
import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import com.ankkun.ecommerce.module.coupon.repository.CouponRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
@RestController @RequestMapping("/api/coupons") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {
    private final CouponRepository coupons;
    @Data public static class Input {
        private String code;
        @JsonProperty("discount_type") private String type;
        @DecimalMin("0.01") @JsonProperty("discount_value") private BigDecimal value;
        @DecimalMin("0") @JsonProperty("min_order_value") private BigDecimal min;
        @Min(1) @JsonProperty("usage_limit") private Integer limit;
        @JsonProperty("is_active") private Boolean active;
        @JsonProperty("expires_at") private Instant expires;
    }
    @GetMapping public Object list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit) {
        page=Math.max(1,page);limit=Math.max(1,Math.min(100,limit));var result=coupons.findAll(PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"createdAt")));
        return ApiResponse.ok(PaginatedResponse.of(result.getContent(),page,limit,result.getTotalElements()));
    }
    @PostMapping @Transactional public Object create(@Valid @RequestBody Input input) {
        var c=Coupon.builder().build();apply(c,input);
        if(c.getCode()==null || c.getCode().isBlank() || c.getDiscountType()==null || c.getDiscountValue()==null) throw new BadRequestException("Thiếu mã hoặc loại/giá trị giảm giá");
        if(coupons.existsByCode(c.getCode())) throw new ConflictException("Mã đã tồn tại");
        return ApiResponse.ok(coupons.saveAndFlush(c));
    }
    @PatchMapping("/{id}") @Transactional public Object update(@PathVariable String id,@Valid @RequestBody Input input) {
        var c=coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found"));
        if(input.code!=null && !input.code.trim().equalsIgnoreCase(c.getCode()) && coupons.existsByCode(input.code.trim().toUpperCase())) throw new ConflictException("Mã đã tồn tại");
        apply(c,input);return ApiResponse.ok(coupons.save(c));
    }
    @DeleteMapping("/{id}") @Transactional public Object delete(@PathVariable String id) {
        var c=coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found"));c.setIsActive(false);return ApiResponse.ok(coupons.save(c));
    }
    private void apply(Coupon c,Input x) {
        if(x.code!=null)c.setCode(x.code.trim().toUpperCase());if(x.type!=null)c.setDiscountType(x.type);
        if(x.value!=null)c.setDiscountValue(x.value);if(x.min!=null)c.setMinOrderValue(x.min);if(x.limit!=null)c.setUsageLimit(x.limit);
        if(x.active!=null)c.setIsActive(x.active);if(x.expires!=null)c.setExpiresAt(x.expires);
        if(c.getDiscountType()!=null && !java.util.Set.of("fixed","percent").contains(c.getDiscountType()))throw new BadRequestException("Loại giảm giá không hợp lệ");
        if("percent".equals(c.getDiscountType()) && c.getDiscountValue()!=null && c.getDiscountValue().compareTo(new BigDecimal("100"))>0) throw new BadRequestException("Phần trăm không được vượt 100");
        if(c.getUsageLimit()<c.getUsedCount())throw new BadRequestException("Giới hạn thấp hơn số lượt đã dùng");
    }
}
