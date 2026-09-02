package com.ankkun.ecommerce.module.product.entity;

import com.ankkun.ecommerce.module.category.entity.Category;
import com.ankkun.ecommerce.module.review.entity.Review;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products",
        indexes = {
                @Index(columnList = "status"),
                @Index(columnList = "min_price"),
                @Index(columnList = "sold_count"),
                @Index(columnList = "view_count")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "category_id")
    private String categoryId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    private String brand;

    @Column(nullable = false)
    @Builder.Default
    private String status = "draft";

    // Store JSON as String (Hibernate will handle it via columnDefinition)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String description = "{}";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String attributes = "{}";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String media = "{}";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String seo = "{}";

    // Stored as text array in PostgreSQL via comma-separated or JSON array
    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "category_path", columnDefinition = "text[]")
    private String[] categoryPath;

    @Column(name = "rating_avg", nullable = false)
    @Builder.Default
    private Double ratingAvg = 0.0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "min_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minPrice = BigDecimal.ZERO;

    @Column(name = "max_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal maxPrice = BigDecimal.ZERO;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "primary_variant_id")
    private String primaryVariantId;

    @Column(name = "attribute_order", columnDefinition = "text[]")
    private String[] attributeOrder;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
