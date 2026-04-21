package com.example.instagramclone.domain.dm.dto;

/**
 * STOMP DM 타이핑 표시 요청. 클라이언트가 /app/dm.typing 으로 보내는 메시지 바디.
 *
 * DB 에 저장하지 않는 휘발성 이벤트로, 연결이 끊기면 그대로 사라진다.
 * senderId 를 담지 않는 이유는 DmSendRequest 와 동일 — 사칭 방지를 위해
 * 발신자는 STOMP Principal 에서만 꺼내 쓴다.
 */
public record DmTypingRequest(
        Long conversationId,
        boolean typing
) {}
