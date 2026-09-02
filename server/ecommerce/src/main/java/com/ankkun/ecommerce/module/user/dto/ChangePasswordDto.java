package com.ankkun.ecommerce.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {
    @NotBlank
    @JsonProperty("current_password")
    private String currentPassword;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu mới phải ít nhất 8 ký tự")
    @JsonProperty("new_password")
    private String newPassword;
}
