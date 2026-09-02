package com.ankkun.ecommerce.module.user.dto;

import com.ankkun.ecommerce.common.enums.UserRole;
import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private boolean active;  // tránh isActive — Lombok generate isActive() trùng pattern boolean
}
