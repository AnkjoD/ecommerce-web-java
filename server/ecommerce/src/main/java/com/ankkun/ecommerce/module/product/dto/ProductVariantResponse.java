package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class ProductVariantResponse {
    private String id;
    private Integer reservedQuantity;
    private Integer soldCount;
    private Integer viewCount;
    private ProductResponse product;
    private String sku;
    private String color;
    private String size;
    private JsonNode options;
    private BigDecimal price;
    private BigDecimal priceBeforeDiscount;
    private Integer stockQuantity;
    private String imageUrl;
}
