package com.ankkun.ecommerce.module.category.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String parentId;
    private String imageUrl;
    private Integer sortOrder;
    @com.fasterxml.jackson.annotation.JsonProperty("is_active") private Boolean active;
    private List<CategoryResponse> children;
}
