package com.example.instagramclone.domain.dm.api;

import com.example.instagramclone.core.common.dto.ApiResponse;
import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.application.DirectMessageService;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.infrastructure.security.annotation.LoginUser;
import com.example.instagramclone.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DM 메시지 이력 REST 조회.
 * WebSocket 실시간 수신의 보조 수단 — 재접속/초기 로드용.
 */
@RestController
@RequiredArgsConstructor
public class DirectMessageController {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final DirectMessageService directMessageService;

    @GetMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<SliceResponse<DirectMessageResponse>>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @LoginUser LoginUserInfoDto loginUser) {

        int clamped = Math.max(1, Math.min(size, MAX_SIZE));

        SliceResponse<DirectMessageResponse> messages = directMessageService.getMessages(
                conversationId, loginUser.id(), cursor, clamped);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 대화방의 읽지 않은 메시지 일괄 읽음 처리.
     * 프론트엔드가 대화방 진입 시 호출하는 것을 전제로 설계했다.
     * 반환값은 실제로 읽음으로 바뀐 메시지 수 — 뱃지 카운트 감산에 사용.
     */
    @PatchMapping("/api/conversations/{conversationId}/messages/read")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(
            @PathVariable Long conversationId,
            @LoginUser LoginUserInfoDto loginUser) {

        int updatedCount =
                directMessageService.markMessagesAsRead(conversationId, loginUser.id());

        return ResponseEntity.ok(ApiResponse.success(updatedCount));
    }
}
