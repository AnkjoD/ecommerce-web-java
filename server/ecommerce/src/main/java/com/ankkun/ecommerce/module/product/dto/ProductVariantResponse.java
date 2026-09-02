package com.ankkun.ecommerce.module.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductVariantResponse {
    private String id;
    private String sku;
    private String color;
    private String size;
    private String options;
    private BigDecimal price;
    private BigDecimal priceBeforeDiscount;
    private Integer stockQuantity;
    private String imageUrl;
}
