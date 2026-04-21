package com.example.instagramclone.domain.dm.api;

import com.example.instagramclone.domain.dm.application.DirectMessageService;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.dm.dto.DmSendRequest;
import com.example.instagramclone.domain.dm.dto.DmTypingRequest;
import com.example.instagramclone.infrastructure.websocket.StompPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.only;

/**
 * DmController 단위 테스트.
 *
 * @MessageMapping 메서드는 POJO 이므로 직접 호출해 계약을 검증한다:
 * - Principal 에서 senderId(Long) 를 추출해 서비스로 위임한다
 * - 서비스가 반환한 DirectMessageResponse 가 @SendToUser 용 반환값으로 그대로 나온다
 *
 * 실제 @SendToUser / convertAndSendToUser 전달 경로는 Spring 에 맡기고,
 * ws-test.html 로 수동 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class DmControllerTest {

    @Mock
    private DirectMessageService directMessageService;

    @InjectMocks
    private DmController dmController;

    @Test
    @DisplayName("sendDm() - Principal 에서 senderId 를 꺼내 서비스에 위임하고 응답을 반환한다")
    void send_dm_delegates_to_service_with_principal_member_id() {
        StompPrincipal principal = new StompPrincipal(42L);
        DmSendRequest request = new DmSendRequest(10L, "안녕!");

        DirectMessageResponse expected = new DirectMessageResponse(
                100L, 10L, 42L, "koo", "안녕!", LocalDateTime.now());
        given(directMessageService.sendMessage(42L, request)).willReturn(expected);

        DirectMessageResponse actual = dmController.sendDm(request, principal);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("typing() - Principal 에서 senderId 를 꺼내 서비스의 broadcastTyping 에만 위임한다")
    void typing_delegates_to_service_with_principal_member_id() {
        StompPrincipal principal = new StompPrincipal(42L);
        DmTypingRequest request = new DmTypingRequest(10L, true);

        dmController.typing(request, principal);

        then(directMessageService).should(only()).broadcastTyping(42L, request);
    }
}
