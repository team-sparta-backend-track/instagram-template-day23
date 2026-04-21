package com.example.instagramclone.domain.dm.api;

import com.example.instagramclone.domain.dm.application.DirectMessageService;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.dm.dto.DmSendRequest;
import com.example.instagramclone.domain.dm.dto.DmTypingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP 기반 DM 메시지 컨트롤러.
 *
 * 보낸 사람(A) 에게는 @SendToUser 로 반환값을 /user/queue/dm 로 돌려주고,
 * 받는 사람(B) 에게는 Service 내부의 convertAndSendToUser 가 같은 경로로 전달한다.
 * 클라이언트는 /user/queue/dm 하나만 구독하면 양쪽 메시지를 모두 받는다.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DmController {

    private final DirectMessageService directMessageService;

    @MessageMapping("/dm.send")
    @SendToUser("/queue/dm")
    public DirectMessageResponse sendDm(@Payload DmSendRequest request,
                                        Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        return directMessageService.sendMessage(senderId, request);
    }

    /**
     * 타이핑 표시 이벤트. DB 저장 없이 상대방의 /user/queue/dm-typing 으로만 푸시된다.
     * 본인은 자신이 입력 중임을 이미 알고 있으므로 @SendToUser 로 에코하지 않는다.
     */
    @MessageMapping("/dm.typing")
    public void typing(@Payload DmTypingRequest request,
                       Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        directMessageService.broadcastTyping(senderId, request);
    }
}
