package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;

@Data
public class ProductCreateDto {
    @NotBlank
    @jakarta.validation.constraints.Size(min=1,max=255) private String name;
    
    private String slug;
    @jakarta.validation.Valid
    private java.util.List<VariantCreateDto> variants;
    
    private String brand;
    
    @JsonProperty("category_id")
    private String categoryId;
    
    @jakarta.validation.constraints.Pattern(regexp="draft|active|archived") private String status = "draft";
    private JsonNode description;
    private JsonNode attributes;
    private JsonNode media;
    private JsonNode tags;
    @JsonProperty("attribute_order") private String[] attributeOrder;
    @JsonProperty("primary_variant_id") private String primaryVariantId;
    private JsonNode seo;
}
