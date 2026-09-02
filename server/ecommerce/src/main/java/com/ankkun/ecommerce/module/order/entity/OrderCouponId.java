package com.ankkun.ecommerce.module.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class OrderCouponId implements Serializable {
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "coupon_id")
    private String couponId;
}
