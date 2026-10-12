package com.ankkun.ecommerce.module.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CategoryUpdateDto {
    @jakarta.validation.constraints.Size(min=1,max=255) private String name;
    @JsonProperty("parent_id") private String parentId;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    @JsonProperty("sort_order")
    private Integer sortOrder;
    
    @JsonProperty("is_active")
    private Boolean active;
}
