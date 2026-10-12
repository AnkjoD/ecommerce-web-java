package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class ProductResponse {
    private String id;
    private com.ankkun.ecommerce.module.category.dto.CategoryResponse category;
    private String[] tags;
    private String[] attributeOrder;
    private String primaryVariantId;
    private Integer viewCount;
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
    private JsonNode description;
    private JsonNode attributes;
    private JsonNode media;
    private JsonNode seo;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductVariantResponse> variants;
}
