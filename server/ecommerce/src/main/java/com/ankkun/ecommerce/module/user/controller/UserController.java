package com.ankkun.ecommerce.module.user.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.user.dto.ChangePasswordDto;
import com.ankkun.ecommerce.module.user.dto.UpdateProfileDto;
import com.ankkun.ecommerce.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(userId)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<?>> updateProfile(@AuthenticationPrincipal String userId,
                                                        @RequestBody UpdateProfileDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(userId, dto)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal String userId,
                                                            @Valid @RequestBody ChangePasswordDto dto) {
        userService.changePassword(userId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }
}
