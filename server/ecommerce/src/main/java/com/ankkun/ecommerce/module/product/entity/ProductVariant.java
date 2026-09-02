package com.ankkun.ecommerce.module.product.entity;

import com.ankkun.ecommerce.module.cart.entity.CartItem;
import com.ankkun.ecommerce.module.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants",
        indexes = @Index(columnList = "product_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(unique = true, nullable = false)
    private String sku;

    private String color;
    private String size;

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String options = "{}";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "price_before_discount", precision = 12, scale = 2)
    private BigDecimal priceBeforeDiscount;

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 500;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @OneToMany(mappedBy = "variant")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "variant")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
