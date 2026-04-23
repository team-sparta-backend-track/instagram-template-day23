package com.example.instagramclone.domain.dm.application;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.dm.domain.DirectMessageRepository;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.dm.dto.DmSendRequest;
import com.example.instagramclone.domain.dm.dto.DmTypingResponse;
import com.example.instagramclone.domain.dm.dto.DmTypingRequest;
import com.example.instagramclone.domain.member.domain.Member;
import com.example.instagramclone.domain.member.domain.MemberRepository;
import com.example.instagramclone.domain.notification.domain.NotificationType;
import com.example.instagramclone.domain.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DirectMessageService {

    private static final String USER_DM_DESTINATION = "/queue/dm";
    private static final String USER_DM_TYPING_DESTINATION = "/queue/dm-typing";

    private final DirectMessageRepository directMessageRepository;
    private final ConversationService conversationService;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * DM 메시지 전송: 권한 검증 → DB 저장 → 상대방에게 WebSocket 실시간 전달.
     *
     * 보낸 사람 본인에게는 여기서 Push 하지 않는다 —
     * STOMP 컨트롤러의 @SendToUser 가 메서드 반환값을 본인 개인 큐로 돌려준다.
     */
    @Transactional
    public DirectMessageResponse sendMessage(Long senderId, DmSendRequest request) {
        Conversation conversation =
                conversationService.getConversationOrThrow(request.conversationId(), senderId);

        Member sender = memberRepository.getReferenceById(senderId);
        DirectMessage message = DirectMessage.create(conversation, sender, request.content());
        directMessageRepository.save(message);

        DirectMessageResponse response = DirectMessageResponse.from(message);

        Member other = conversation.getOtherParticipant(senderId);

        // 상대방에게 실시간으로 메시지 전달
        messagingTemplate.convertAndSendToUser(
                String.valueOf(other.getId()),
                USER_DM_DESTINATION,
                response
        );

        // 대화방 밖에서도 "새 메시지가 있어요" 뱃지를 띄울 수 있도록 알림 이벤트를 발행한다.
        // @TransactionalEventListener 가 트랜잭션 커밋 후 비동기로 DB 저장 + WebSocket Push.
        // DM 은 항상 sender != receiver 이므로 isSelfNotification 에 자동으로 안 걸린다.
        eventPublisher.publishEvent(new NotificationEvent(
                NotificationType.DM,
                other.getId(),
                senderId,
                conversation.getId(),   // targetId = conversationId (탭 시 대화방 진입)
                null                    // message 는 핸들러의 buildMessage 에서 생성
        ));

        log.info("[DM] {} → {} (conversationId={})",
                senderId, other.getId(), conversation.getId());

        return response;
    }

    /**
     * 타이핑 표시 브로드캐스트: 권한 검증 → 상대방에게만 휘발성 이벤트 전달.
     *
     * DB INSERT 없음. 재연결 시 복구 로직도 없고, 수신 측은 짧은 TTL 로 자체 소거한다.
     * 참여자 검증을 sendMessage 와 동일하게 수행해 비참여자의 개인 큐 스팸을 막는다.
     */
    public void broadcastTyping(Long senderId, DmTypingRequest request) {
        Conversation conversation =
                conversationService.getConversationOrThrow(request.conversationId(), senderId);

        Member me = memberRepository.getReferenceById(senderId);
        Member other = conversation.getOtherParticipant(senderId);

        DmTypingResponse payload = new DmTypingResponse(
                conversation.getId(),
                senderId,
                me.getUsername(),
                request.typing()
        );

        messagingTemplate.convertAndSendToUser(
                String.valueOf(other.getId()),
                USER_DM_TYPING_DESTINATION,
                payload
        );
    }

    /**
     * DM 이력 조회 (커서 페이지네이션, 최신 → 과거).
     * WebSocket 이 끊겼을 때의 보조 수단이자 대화방 진입 시 초기 로딩 소스.
     */
    public SliceResponse<DirectMessageResponse> getMessages(Long conversationId,
                                                            Long loginMemberId,
                                                            Long cursorId,
                                                            int size) {
        conversationService.getConversationOrThrow(conversationId, loginMemberId);
        return directMessageRepository.findMessages(conversationId, cursorId, size);
    }

    /**
     * 대화방의 읽지 않은 메시지를 일괄 읽음 처리. 참여자만 호출 가능.
     * 프론트엔드가 대화방 진입 시 한 번 호출하는 것을 전제로 한다.
     *
     * @return 실제로 읽음으로 바뀐 메시지 수 — UI 뱃지 카운트 감산에 사용
     */
    @Transactional
    public int markMessagesAsRead(Long conversationId, Long loginMemberId) {
        conversationService.getConversationOrThrow(conversationId, loginMemberId);
        return directMessageRepository.markAllAsRead(conversationId, loginMemberId);
    }
}
