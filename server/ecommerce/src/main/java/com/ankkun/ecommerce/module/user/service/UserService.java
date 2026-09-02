package com.ankkun.ecommerce.module.user.service;

import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.auth.repository.RefreshTokenRepository;
import com.ankkun.ecommerce.module.user.dto.ChangePasswordDto;
import com.ankkun.ecommerce.module.user.dto.UpdateProfileDto;
import com.ankkun.ecommerce.module.user.dto.UserResponse;
import com.ankkun.ecommerce.module.user.entity.User;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        return toResponse(user);
    }

    public static UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setActive(user.getIsActive());
        return r;
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileDto dto) {
        if (dto.getPhone() != null && userRepository.existsByPhoneAndIdNot(dto.getPhone(), userId)) {
            throw new BadRequestException("Số điện thoại này đã được đăng ký bởi một tài khoản khác");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        // revoke all refresh tokens after password change
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // Admin
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User toggleActive(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }
}
