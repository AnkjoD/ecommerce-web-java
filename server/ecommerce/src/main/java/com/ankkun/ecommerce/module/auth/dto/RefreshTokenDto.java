package com.ankkun.ecommerce.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDto {
    @NotBlank
    @JsonProperty("refresh_token")
    private String refreshToken;
}
