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
        r.setFullName(user.getFullName());r.setAvatar(user.getAvatar()==null?"/assets/demo-avatar.svg":user.getAvatar());
        r.setDateOfBirth(user.getDateOfBirth());r.setGender(user.getGender());r.setCreatedAt(user.getCreatedAt());r.setUpdatedAt(user.getUpdatedAt());
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
        if(dto.getAvatar()!=null){if(!dto.getAvatar().equals("/assets/demo-avatar.svg")&&!dto.getAvatar().matches("/api/files/[a-f0-9-]+\\.(png|jpg|webp)"))throw new BadRequestException("Ảnh đại diện không hợp lệ");user.setAvatar(dto.getAvatar());}
        if(dto.getDateOfBirth()!=null)user.setDateOfBirth(dto.getDateOfBirth());if(dto.getGender()!=null)user.setGender(dto.getGender());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordDto dto) {
        User user = userRepository.findLocked(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion()+1);
        userRepository.save(user);
        // revoke all refresh tokens after password change
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // Admin
    public Page<User> getAllUsers(Pageable pageable,String search) {
        return userRepository.findAll((root,query,cb)->{if(search==null||search.isBlank())return cb.conjunction();String q="%"+search.toLowerCase()+"%";return cb.or(cb.like(cb.lower(root.get("email")),q),cb.like(cb.lower(root.get("fullName")),q));},pageable);
    }

    @Transactional
    public User toggleActive(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        user.setIsActive(!user.getIsActive());user.setTokenVersion(user.getTokenVersion()+1);refreshTokenRepository.revokeAllByUserId(userId);
        return userRepository.save(user);
    }
}
