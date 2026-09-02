package com.ankkun.ecommerce.module.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateDto {
    @NotBlank
    private String name;
    
    @NotBlank
    private String slug;
    
    @JsonProperty("parent_id")
    private String parentId;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    @JsonProperty("sort_order")
    private Integer sortOrder = 0;
}
