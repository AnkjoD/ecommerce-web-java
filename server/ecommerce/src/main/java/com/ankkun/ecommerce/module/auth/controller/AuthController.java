package com.ankkun.ecommerce.module.auth.controller;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.common.exception.UnauthorizedException;
import com.ankkun.ecommerce.module.auth.dto.*;
import com.ankkun.ecommerce.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @Value("${app.cookie-secure:false}") private boolean secureCookies;

    private ResponseEntity<ApiResponse<AuthUserDto>> session(AuthResponse auth, HttpStatus status, boolean admin) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(admin ? "adminAccessToken" : "accessToken", auth.getTokens().getAccess_token(), "/api", Duration.ofMinutes(15)))
                .header(HttpHeaders.SET_COOKIE, cookie(admin ? "adminRefreshToken" : "refreshToken", auth.getTokens().getRefresh_token(), "/api/auth", Duration.ofDays(7)))
                .body(ApiResponse.ok(auth.getUser()));
    }
    private String cookie(String name, String value, String path, Duration age) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secureCookies)
                .sameSite("Strict").path(path).maxAge(age).build().toString();
    }
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthUserDto>> register(@Valid @RequestBody RegisterDto dto) {
        return session(authService.register(dto), HttpStatus.CREATED, false);
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUserDto>> login(@Valid @RequestBody LoginDto dto) {
        return session(authService.login(dto), HttpStatus.OK, false);
    }
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthUserDto>> loginAdmin(@Valid @RequestBody LoginDto dto) {
        return session(authService.loginAdmin(dto), HttpStatus.OK, true);
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserDto>> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(userId)));
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal String userId, @RequestHeader(value="X-Client-App", defaultValue="customer") String app) {
        authService.logout(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("admin".equals(app) ? "adminAccessToken" : "accessToken", "", "/api", Duration.ZERO))
                .header(HttpHeaders.SET_COOKIE, cookie("admin".equals(app) ? "adminRefreshToken" : "refreshToken", "", "/api/auth", Duration.ZERO))
                .body(ApiResponse.ok("Logged out", null));
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthUserDto>> refresh(@CookieValue(name="refreshToken", required=false) String customerToken,
            @CookieValue(name="adminRefreshToken", required=false) String adminToken,
            @RequestHeader(value="X-Client-App", defaultValue="customer") String app) {
        boolean admin = "admin".equals(app);
        String token = admin ? adminToken : customerToken;
        if (token == null || token.isBlank()) throw new UnauthorizedException("Session expired");
        return session(authService.refreshTokens(token), HttpStatus.OK, admin);
    }
}
