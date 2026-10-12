package com.ankkun.ecommerce.module.cart.dto;

import com.ankkun.ecommerce.module.product.dto.ProductVariantResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class CartResponse {
    private String id;
    private java.math.BigDecimal subtotal;
    private int totalItems;
    private String userId;
    private List<CartItemResponse> items;
}
