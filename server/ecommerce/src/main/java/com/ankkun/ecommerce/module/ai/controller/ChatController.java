package com.ankkun.ecommerce.module.ai.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.ai.dto.ChatRequest;
import com.ankkun.ecommerce.module.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat/completions
     * Bắt buộc đăng nhập — JwtAuthFilter đã handle.
     */
    @PostMapping("/completions")
    public ApiResponse<Object> chatCompletion(
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRequest req
    ) {
        var result = chatService.chatCompletion(userId, req.getMessage(), req.getChatHistory());
        return ApiResponse.ok(result);
    }
}
