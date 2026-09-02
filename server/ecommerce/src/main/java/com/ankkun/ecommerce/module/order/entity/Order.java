package com.ankkun.ecommerce.module.order.entity;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentMethod;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import com.ankkun.ecommerce.module.address.entity.Address;
import com.ankkun.ecommerce.module.review.entity.Review;
import com.ankkun.ecommerce.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders",
        indexes = {
                @Index(columnList = "user_id"),
                @Index(columnList = "status"),
                @Index(columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "address_id", nullable = false)
    private String addressId;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.unpaid;

    @Column(name = "ghtk_label", unique = true)
    private String ghtkLabel;

    @Column(name = "shipping_log", columnDefinition = "jsonb")
    private String shippingLog;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", insertable = false, updatable = false)
    private Address address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderCoupon> coupons = new ArrayList<>();

    @OneToMany(mappedBy = "order")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
