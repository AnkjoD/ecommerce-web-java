package com.ankkun.ecommerce.module.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotBlank
    @JsonProperty("address_id")
    private String addressId;
    
    @NotBlank
    @JsonProperty("payment_method")
    private String paymentMethod;
}
