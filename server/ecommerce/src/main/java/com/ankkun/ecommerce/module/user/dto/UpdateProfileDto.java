package com.ankkun.ecommerce.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateProfileDto {
    @JsonProperty("full_name")
    private String fullName;
    private String phone;
}
