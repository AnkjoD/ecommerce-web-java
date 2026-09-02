package com.ankkun.ecommerce.module.cart.dto;

import com.ankkun.ecommerce.module.product.dto.ProductVariantResponse;
import lombok.Data;
import java.util.List;

@Data
public class CartResponse {
    private String id;
    private String userId;
    private List<CartItemResponse> items;
}
