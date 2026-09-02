package com.ankkun.ecommerce.module.user.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int limit) {
        var result = userService.getAllUsers(PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<?>> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.toggleActive(id)));
    }
}
