package com.ankkun.ecommerce.module.auth.service;

import com.ankkun.ecommerce.common.enums.UserRole;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.common.exception.ConflictException;
import com.ankkun.ecommerce.common.exception.ForbiddenException;
import com.ankkun.ecommerce.common.exception.UnauthorizedException;
import com.ankkun.ecommerce.module.auth.dto.*;
import com.ankkun.ecommerce.module.auth.entity.RefreshToken;
import com.ankkun.ecommerce.module.auth.repository.RefreshTokenRepository;
import com.ankkun.ecommerce.module.user.entity.User;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import com.ankkun.ecommerce.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        if (dto.getPhone() != null && userRepository.existsByPhone(dto.getPhone())) {
            throw new ConflictException("Số điện thoại đã được đăng ký bởi một tài khoản khác");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .role(UserRole.customer)
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Access Denied"));

        if (!user.getIsActive()) throw new UnauthorizedException("Access Denied");
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Access Denied");
        }
        if (user.getRole() == UserRole.admin) {
            throw new UnauthorizedException("Vui lòng sử dụng cổng đăng nhập dành cho Quản trị viên");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginAdmin(LoginDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Access Denied"));

        if (!user.getIsActive() || user.getRole() != UserRole.admin) {
            throw new UnauthorizedException("Access Denied");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Access Denied");
        }

        return buildAuthResponse(user);
    }

    public AuthUserDto getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Người dùng không tồn tại hoặc đã bị xóa"));
        if (!user.getIsActive()) throw new UnauthorizedException("Tài khoản của bạn đã bị khóa");
        return mapToAuthUser(user);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public AuthResponse refreshTokens(String refreshToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new ForbiddenException("Access Denied");
        }

        String userId = claims.getSubject();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Access Denied"));
        if (!user.getIsActive()) throw new ForbiddenException("Access Denied");

        String tokenHash = hash(refreshToken);
        RefreshToken rt = refreshTokenRepository
                .findByTokenHashAndIsRevokedFalseAndExpiresAtAfter(tokenHash, Instant.now())
                .orElseThrow(() -> new ForbiddenException("Access Denied"));

        // Token rotation
        rt.setIsRevoked(true);
        refreshTokenRepository.save(rt);

        return buildAuthResponse(user);
    }

    @Transactional
    public Object seedAdmin() {
        User first = userRepository.findAll().stream()
                .min((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElseThrow(() -> new BadRequestException("Chưa có người dùng nào trong hệ thống"));

        first.setRole(UserRole.admin);
        userRepository.save(first);
        return java.util.Map.of("message", "Đã nâng cấp tài khoản đầu tiên lên Admin", "email", first.getEmail());
    }

    // ---- private helpers ----

    private AuthResponse buildAuthResponse(User user) {
        String at = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rt = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());
        saveRefreshToken(user.getId(), rt);
        return new AuthResponse(new TokensDto(at, rt), mapToAuthUser(user));
    }

    private void saveRefreshToken(String userId, String rt) {
        refreshTokenRepository.revokeAllByUserId(userId);
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rt))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        try {
            refreshTokenRepository.save(token);
        } catch (Exception ignored) {
            // unique constraint violation — race condition safe to ignore
        }
    }

    private AuthUserDto mapToAuthUser(User user) {
        String avatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getId();
        return new AuthUserDto(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), avatar);
    }

    private String hash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
