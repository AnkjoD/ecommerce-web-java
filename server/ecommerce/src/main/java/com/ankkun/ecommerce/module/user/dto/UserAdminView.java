package com.ankkun.ecommerce.module.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserAdminView {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private LocalDateTime createdAt;
    private int orderCount;
}
