package com.ankkun.ecommerce.module.cart.entity;

import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "variant_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "cart_id", nullable = false)
    private String cartId;

    @Column(name = "variant_id", nullable = false)
    private String variantId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "added_at", updatable = false)
    @Builder.Default
    private Instant addedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", insertable = false, updatable = false)
    private ProductVariant variant;
}
