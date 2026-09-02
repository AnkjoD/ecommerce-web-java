package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantCreateDto {
    @NotBlank
    private String sku;
    private String color;
    private String size;
    private String options = "{}";
    
    @NotNull
    @Min(0)
    private BigDecimal price;
    
    @JsonProperty("price_before_discount")
    private BigDecimal priceBeforeDiscount;
    
    @NotNull
    @Min(0)
    @JsonProperty("stock_quantity")
    private Integer stockQuantity;
    
    @JsonProperty("image_url")
    private String imageUrl;
}
