package com.example.instagramclone.domain.dm.dto;

import com.example.instagramclone.domain.dm.domain.DirectMessage;

import java.time.LocalDateTime;

/**
 * DM 메시지 응답.
 * STOMP 실시간 전달 + REST 이력 조회 양쪽에서 공용으로 사용.
 */
public record DirectMessageResponse(
        Long messageId,
        Long conversationId,
        Long senderId,
        String senderUsername,
        String content,
        LocalDateTime createdAt
) {
    public static DirectMessageResponse from(DirectMessage dm) {
        return new DirectMessageResponse(
                dm.getId(),
                dm.getConversation().getId(),
                dm.getSender().getId(),
                dm.getSender().getUsername(),
                dm.getContent(),
                dm.getCreatedAt()
        );
    }
}
