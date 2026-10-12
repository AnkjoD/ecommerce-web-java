package com.ankkun.ecommerce.module.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CartItemUpdateRequest {
    @jakarta.validation.constraints.Min(0)
    private int quantity;
}
