package com.ankkun.ecommerce.module.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CartItemUpdateRequest {
    private int quantity;
}
