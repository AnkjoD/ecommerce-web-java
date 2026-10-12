package com.ankkun.ecommerce.module.coupon.service;
import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import com.ankkun.ecommerce.module.coupon.repository.CouponRepository;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository repository;
    public List<Coupon> active() { return repository.findAll().stream().filter(this::available).toList(); }
    private boolean available(Coupon c) {
        return c.getIsActive() && c.getUsedCount() < c.getUsageLimit()
                && (c.getExpiresAt() == null || c.getExpiresAt().isAfter(Instant.now()));
    }
    public BigDecimal discount(Coupon c, BigDecimal subtotal) {
        if (!available(c) || subtotal.compareTo(c.getMinOrderValue()) < 0)
            throw new BadRequestException("Mã giảm giá không hợp lệ hoặc đơn hàng chưa đạt giá trị tối thiểu");
        return ("percent".equals(c.getDiscountType())
                ? subtotal.multiply(c.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : c.getDiscountValue()).min(subtotal).max(BigDecimal.ZERO);
    }
    public Coupon validate(String code) {
        return repository.findByCodeAndIsActiveTrue(code.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
    }
    @Transactional
    public BigDecimal consume(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        Coupon c = repository.findLockedByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
        BigDecimal amount = discount(c, subtotal);
        c.setUsedCount(c.getUsedCount() + 1);
        repository.save(c);
        return amount;
    }
}
