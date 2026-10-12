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
    @JsonProperty("coupon_code")
    private String couponCode;
    @JsonProperty("item_ids")
    @jakarta.validation.constraints.Size(min=1, max=100)
    private java.util.List<@jakarta.validation.constraints.NotBlank String> itemIds;
}
