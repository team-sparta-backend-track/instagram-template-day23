package com.example.instagramclone.domain.dm.api;

import com.example.instagramclone.core.common.dto.ApiResponse;
import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.application.ConversationService;
import com.example.instagramclone.domain.dm.dto.ConversationResponse;
import com.example.instagramclone.infrastructure.security.annotation.LoginUser;
import com.example.instagramclone.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConversationController {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ConversationService conversationService;

    @PostMapping("/api/conversations/{targetMemberId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @PathVariable Long targetMemberId,
            @LoginUser LoginUserInfoDto loginUser) {

        ConversationResponse response =
                conversationService.getOrCreateConversation(loginUser.id(), targetMemberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/conversations")
    public ResponseEntity<ApiResponse<SliceResponse<ConversationResponse>>> getMyConversations(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @LoginUser LoginUserInfoDto loginUser) {

        int clamped = Math.max(1, Math.min(size, MAX_SIZE));

        SliceResponse<ConversationResponse> response =
                conversationService.getMyConversations(loginUser.id(), cursor, clamped);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 대화방 삭제 (나가기).
     * DELETE /api/conversations/{conversationId}
     * - 참여자만 삭제 가능 (서비스에서 권한 검증)
     * - 대화방 + 메시지 하드 삭제
     */
    @DeleteMapping("/api/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @LoginUser LoginUserInfoDto loginUser) {

        conversationService.deleteConversation(conversationId, loginUser.id());
        return ResponseEntity.noContent().build();
    }
}
