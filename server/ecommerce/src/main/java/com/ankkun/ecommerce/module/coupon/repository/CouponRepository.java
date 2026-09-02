package com.ankkun.ecommerce.module.coupon.repository;

import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCodeAndIsActiveTrue(String code);
    boolean existsByCode(String code);
}
