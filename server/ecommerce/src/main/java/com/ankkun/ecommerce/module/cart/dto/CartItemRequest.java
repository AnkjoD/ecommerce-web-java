package com.ankkun.ecommerce.module.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotBlank
    @JsonProperty("variant_id")
    private String variantId;

    @Min(1)
    private int quantity;
}
