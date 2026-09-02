package com.ankkun.ecommerce.module.category.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String parentId;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;
    private List<CategoryResponse> children;
}
