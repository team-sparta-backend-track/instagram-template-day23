package com.example.instagramclone.domain.dm.dto;

/**
 * STOMP DM 전송 요청. 클라이언트가 /app/dm.send 로 보내는 메시지 바디.
 *
 * senderId 를 담지 않는 이유: 클라이언트가 조작할 수 있으므로 절대 신뢰하지 않는다.
 * 발신자는 STOMP Principal(서버가 CONNECT 시점에 JWT 에서 추출) 에서 꺼내 쓴다.
 */
public record DmSendRequest(
        Long conversationId,
        String content
) {}
