package com.example.instagramclone.infrastructure.websocket;

import com.example.instagramclone.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 프레임의 Authorization 헤더에서 JWT 를 추출해 검증한다.
 *
 * - 검증 실패 → MessageDeliveryException 으로 세션 생성 자체를 막는다.
 * - 검증 성공 → memberId 를 담은 StompPrincipal 을 세션에 바인딩해
 *   이후 @MessageMapping 핸들러의 Principal 파라미터로 꺼내 쓰게 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractBearerToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("[STOMP] 인증 실패: 유효하지 않은 토큰");
            throw new MessageDeliveryException("유효하지 않은 토큰입니다.");
        }

        Long memberId = jwtTokenProvider.getMemberId(token);
        accessor.setUser(new StompPrincipal(memberId));
        log.info("[STOMP] 인증 성공: memberId={}", memberId);

        return message;
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
