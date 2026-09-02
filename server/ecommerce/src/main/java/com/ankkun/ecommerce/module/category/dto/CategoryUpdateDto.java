package com.ankkun.ecommerce.module.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CategoryUpdateDto {
    private String name;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    @JsonProperty("sort_order")
    private Integer sortOrder;
    
    @JsonProperty("is_active")
    private Boolean active;
}
