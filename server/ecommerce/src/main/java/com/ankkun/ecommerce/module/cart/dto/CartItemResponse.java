package com.ankkun.ecommerce.module.cart.dto;

import com.ankkun.ecommerce.module.product.dto.ProductVariantResponse;
import lombok.Data;

@Data
public class CartItemResponse {
    private String id;
    private String variantId;
    private int quantity;
    private ProductVariantResponse variant;
}
