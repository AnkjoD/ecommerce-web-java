package com.ankkun.ecommerce.module.coupon.repository;

import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCodeAndIsActiveTrue(String code);
    boolean existsByCode(String code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Coupon c where c.code = :code and c.isActive = true")
    Optional<Coupon> findLockedByCode(@org.springframework.data.repository.query.Param("code") String code);
}
