package com.example.instagramclone.domain.dm.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StompController 단위 테스트.
 * @MessageMapping 메서드는 POJO 이므로 직접 호출해 검증할 수 있다.
 * 실제 WebSocket/STOMP 브로커 흐름은 ws-test.html 로 수동 확인한다.
 */
class StompControllerTest {

    @Test
    @DisplayName("handlePing() - 입력 메시지를 'PONG: ' 접두어와 함께 돌려준다")
    void handle_ping_echoes_with_pong_prefix() {
        StompController controller = new StompController();

        StompController.PingRequest request = new StompController.PingRequest("Hello STOMP!");
        long before = System.currentTimeMillis();
        StompController.PongResponse response = controller.handlePing(request);
        long after = System.currentTimeMillis();

        assertThat(response.response()).isEqualTo("PONG: Hello STOMP!");
        assertThat(response.timestamp()).isBetween(before, after);
    }
}
