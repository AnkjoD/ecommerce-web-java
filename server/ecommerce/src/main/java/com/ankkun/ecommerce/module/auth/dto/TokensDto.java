package com.ankkun.ecommerce.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokensDto {
    private String access_token;
    private String refresh_token;
}
