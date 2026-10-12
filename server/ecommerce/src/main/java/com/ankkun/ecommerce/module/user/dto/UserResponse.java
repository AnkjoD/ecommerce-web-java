package com.ankkun.ecommerce.module.user.dto;

import com.ankkun.ecommerce.common.enums.UserRole;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class UserResponse {
    private String id;
    private String avatar;
    private java.time.LocalDate dateOfBirth;
    private String gender;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    @com.fasterxml.jackson.annotation.JsonProperty("is_active") private boolean active;  // tránh isActive — Lombok generate isActive() trùng pattern boolean
}
