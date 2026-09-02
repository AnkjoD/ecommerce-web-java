package com.ankkun.ecommerce.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private TokensDto tokens;
    private AuthUserDto user;
}
