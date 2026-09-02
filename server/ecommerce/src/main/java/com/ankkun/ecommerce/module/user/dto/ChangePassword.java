package com.ankkun.ecommerce.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChangePassword {
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;
}