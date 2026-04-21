package com.example.instagramclone.domain.dm.dto;

/**
 * DM 타이핑 표시 응답.
 * 대화 상대방의 /user/queue/dm-typing 으로만 전달되는 1:1 휘발성 이벤트.
 * DirectMessageResponse 와 구조가 다르므로 /queue/dm 과 채널을 분리한다.
 */
public record DmTypingResponse(
        Long conversationId,
        Long senderId,
        String senderUsername,
        boolean typing
) {}
