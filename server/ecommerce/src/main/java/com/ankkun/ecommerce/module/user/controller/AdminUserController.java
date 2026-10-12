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
                                                 @RequestParam(defaultValue = "20") int limit,@RequestParam(required=false)String search) {
        page=Math.max(1,page);limit=Math.max(1,Math.min(100,limit));
        var result = userService.getAllUsers(PageRequest.of(page - 1, limit),search);
        return ResponseEntity.ok(ApiResponse.ok(com.ankkun.ecommerce.common.dto.PaginatedResponse.of(result.getContent().stream().map(UserService::toResponse).toList(), page, limit, result.getTotalElements())));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<?>> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(UserService.toResponse(userService.toggleActive(id))));
    }
}
