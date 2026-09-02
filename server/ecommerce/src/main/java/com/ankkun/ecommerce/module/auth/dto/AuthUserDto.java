package com.ankkun.ecommerce.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserDto {
    private String id;
    private String email;
    private String full_name;
    private String role;
    private String avatar;
}
