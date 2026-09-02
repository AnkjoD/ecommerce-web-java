package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductCreateDto {
    @NotBlank
    private String name;
    
    @NotBlank
    private String slug;
    
    private String brand;
    
    @JsonProperty("category_id")
    private String categoryId;
    
    private String status = "draft";
    private String description = "{}";
    private String attributes = "{}";
    private String media = "{}";
    private String seo = "{}";
}
