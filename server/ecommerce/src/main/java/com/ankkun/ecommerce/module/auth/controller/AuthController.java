package com.ankkun.ecommerce.module.auth.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.auth.dto.*;
import com.ankkun.ecommerce.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(dto)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(dto)));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginAdmin(dto)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserDto>> getMe(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(userId)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshTokens(dto.getRefreshToken())));
    }

    @PostMapping("/seed-admin")
    public ResponseEntity<ApiResponse<Object>> seedAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(authService.seedAdmin()));
    }
}
