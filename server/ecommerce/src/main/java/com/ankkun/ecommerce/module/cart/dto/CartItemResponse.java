package com.ankkun.ecommerce.module.cart.dto;

import com.ankkun.ecommerce.module.product.dto.ProductVariantResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class CartItemResponse {
    private String id;
    private String variantId;
    private int quantity;
    private ProductVariantResponse variant;
}
