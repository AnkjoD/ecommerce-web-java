package com.ankkun.ecommerce.module.order.entity;

import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCoupon {

    @EmbeddedId
    private OrderCouponId id;

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId")
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("couponId")
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;
}
