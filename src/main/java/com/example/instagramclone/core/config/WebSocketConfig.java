package com.example.instagramclone.core.config;

import com.example.instagramclone.infrastructure.websocket.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 메시지 브로커 설정.
 *
 * - enableSimpleBroker: 서버 내장 메모리 브로커. /topic 은 1:N 브로드캐스트,
 *   /queue 는 1:1 전달용 목적지 접두사로 쓴다. (운영 단계에서는 Redis Pub/Sub 등으로 교체)
 * - setApplicationDestinationPrefixes: 클라이언트 → 서버 전송 경로(/app/...).
 *   이 접두어로 들어오면 @MessageMapping 이 매핑된다.
 * - addEndpoint("/ws").withSockJS(): WebSocket 미지원 브라우저를 위한 폴백 포함.
 * - configureClientInboundChannel: 클라이언트 → 서버 방향 STOMP 프레임(CONNECT/SEND 등)에
 *   JWT 검증 인터셉터를 끼워 넣는다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }
}
