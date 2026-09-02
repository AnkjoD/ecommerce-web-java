package com.ankkun.ecommerce.module.address.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAddressDto {
    @NotBlank private String recipient_name;
    @NotBlank private String phone;
    @NotBlank private String province;
    @NotBlank private String district;
    @NotBlank private String ward;
    @NotBlank private String street;
    private Boolean is_default;
}
