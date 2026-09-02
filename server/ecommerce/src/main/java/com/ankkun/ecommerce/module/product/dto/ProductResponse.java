package com.ankkun.ecommerce.module.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class ProductResponse {
    private String id;
    private String name;
    private String slug;
    private String brand;
    private String categoryId;
    private String status;
    private Double ratingAvg;       // khớp Product.ratingAvg (Double)
    private Integer ratingCount;    // khớp Product.ratingCount (Integer)
    private Integer soldCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String description;
    private String attributes;
    private String media;
    private String seo;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductVariantResponse> variants;
}
