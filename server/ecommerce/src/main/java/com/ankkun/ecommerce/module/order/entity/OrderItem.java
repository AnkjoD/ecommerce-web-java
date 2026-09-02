package com.ankkun.ecommerce.module.order.entity;

import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items",
        indexes = {
                @Index(columnList = "order_id"),
                @Index(columnList = "variant_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "variant_id", nullable = false)
    private String variantId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_info")
    private String variantInfo;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", insertable = false, updatable = false)
    private ProductVariant variant;
}
