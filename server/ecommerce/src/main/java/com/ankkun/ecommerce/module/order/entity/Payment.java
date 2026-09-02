package com.ankkun.ecommerce.module.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(columnList = "provider_order_id"),
                @Index(columnList = "status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
}
