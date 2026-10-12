package com.ankkun.ecommerce.module.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;

@Data
public class ProductUpdateDto {
    @jakarta.validation.constraints.Size(min=1,max=255) private String name;
    private String brand;
    @JsonProperty("category_id")
    private String categoryId;
    @jakarta.validation.constraints.Pattern(regexp="draft|active|archived") private String status;
    private JsonNode description;
    private JsonNode attributes;
    private JsonNode seo;
    private JsonNode media;
    private JsonNode tags;
    @jakarta.validation.Valid private java.util.List<VariantCreateDto> variants;
    @JsonProperty("attribute_order") private String[] attributeOrder;
    @JsonProperty("primary_variant_id") private String primaryVariantId;
}
