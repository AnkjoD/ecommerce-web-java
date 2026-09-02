package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProductUpdateDto {
    private String name;
    private String brand;
    @JsonProperty("category_id")
    private String categoryId;
    private String status;
    private String description;
    private String attributes;
    private String media;
}
