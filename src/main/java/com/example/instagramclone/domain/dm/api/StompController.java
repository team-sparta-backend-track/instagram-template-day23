package com.example.instagramclone.domain.dm.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * STOMP Ping-Pong 핸들러.
 *
 * 클라이언트가 /app/ping 으로 SEND 하면
 * /topic/pong 구독 중인 모든 클라이언트에게 응답을 브로드캐스트한다.
 */
@Controller
@Slf4j
public class StompController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public PongResponse handlePing(PingRequest request) {
        log.info("[STOMP] ping 수신: {}", request.message());
        return new PongResponse(
                "PONG: " + request.message(),
                System.currentTimeMillis()
        );
    }

    public record PingRequest(String message) {}
    public record PongResponse(String response, long timestamp) {}
}
