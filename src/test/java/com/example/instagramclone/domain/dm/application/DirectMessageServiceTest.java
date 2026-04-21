package com.example.instagramclone.domain.dm.application;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.core.exception.ConversationErrorCode;
import com.example.instagramclone.core.exception.ConversationException;
import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.dm.domain.DirectMessageRepository;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.dm.dto.DmSendRequest;
import com.example.instagramclone.domain.member.domain.Member;
import com.example.instagramclone.domain.member.domain.MemberRepository;
import com.example.instagramclone.domain.notification.domain.NotificationType;
import com.example.instagramclone.domain.notification.event.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * DirectMessageService 단위 테스트 — sendMessage() 흐름.
 *
 * [테스트 범위]
 * - 참여자가 아닐 때: ConversationService 가 던지는 예외가 그대로 전파된다.
 *   (저장도, Push 도 수행되지 않아야 함)
 * - 정상 흐름: DB 저장 후 상대방에게만 convertAndSendToUser 로 Push.
 *   (자기 자신에게는 보내지 않음 — Step 4의 @SendToUser 가 담당)
 */
@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DirectMessageService directMessageService;

    private Member buildMember(Long id, String username) {
        Member member = Member.builder()
                .username(username)
                .password("pw")
                .email(username + "@test.com")
                .name("이름")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Conversation buildConversation(Long id, Member a, Member b) {
        Conversation conversation = Conversation.create(a, b);
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("실패 - 대화방 참여자가 아니면 예외, 저장/Push 수행되지 않는다")
        void fail_when_not_participant() {
            Long senderId = 99L;
            DmSendRequest request = new DmSendRequest(10L, "안녕!");

            given(conversationService.getConversationOrThrow(10L, senderId))
                    .willThrow(new ConversationException(
                            ConversationErrorCode.CONVERSATION_ACCESS_DENIED));

            assertThatThrownBy(() -> directMessageService.sendMessage(senderId, request))
                    .isInstanceOf(ConversationException.class);

            then(directMessageRepository).should(never()).save(any());
            then(messagingTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - DB 저장 후 상대방에게만 /queue/dm 으로 Push")
        void success_saves_and_pushes_to_other_only() {
            Long senderId = 1L;
            Member me = buildMember(senderId, "koo");
            Member other = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, me, other);

            given(conversationService.getConversationOrThrow(10L, senderId))
                    .willReturn(conversation);
            given(memberRepository.getReferenceById(senderId)).willReturn(me);

            DmSendRequest request = new DmSendRequest(10L, "안녕!");

            DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

            // 1. DB 저장이 호출되었는가
            ArgumentCaptor<DirectMessage> savedCaptor = ArgumentCaptor.forClass(DirectMessage.class);
            then(directMessageRepository).should().save(savedCaptor.capture());
            DirectMessage saved = savedCaptor.getValue();
            assertThat(saved.getContent()).isEqualTo("안녕!");
            assertThat(saved.getSender()).isSameAs(me);
            assertThat(saved.getConversation()).isSameAs(conversation);

            // 2. 상대방에게만 Push — 자신에게는 보내지 않음
            then(messagingTemplate).should().convertAndSendToUser(
                    "2",            // 상대방 memberId 문자열
                    "/queue/dm",
                    response
            );

            // 3. 응답 본문
            assertThat(response.conversationId()).isEqualTo(10L);
            assertThat(response.senderId()).isEqualTo(1L);
            assertThat(response.senderUsername()).isEqualTo("koo");
            assertThat(response.content()).isEqualTo("안녕!");
        }

        @Test
        @DisplayName("성공 - DM 알림 이벤트를 발행한다 (receiver=상대, target=conversationId)")
        void publishes_dm_notification_event() {
            Long senderId = 1L;
            Member me = buildMember(senderId, "koo");
            Member other = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, me, other);

            given(conversationService.getConversationOrThrow(10L, senderId))
                    .willReturn(conversation);
            given(memberRepository.getReferenceById(senderId)).willReturn(me);

            DmSendRequest request = new DmSendRequest(10L, "안녕!");

            directMessageService.sendMessage(senderId, request);

            ArgumentCaptor<NotificationEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            NotificationEvent event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(NotificationType.DM);
            assertThat(event.receiverId()).isEqualTo(2L);       // 상대방
            assertThat(event.senderId()).isEqualTo(1L);         // 보낸 사람
            assertThat(event.targetId()).isEqualTo(10L);        // 대화방 ID
            assertThat(event.isSelfNotification()).isFalse();   // DM은 항상 상대방 → 자동 필터링 통과
        }

        @Test
        @DisplayName("실패 - 권한 검증이 실패하면 이벤트도 발행되지 않는다")
        void no_event_when_permission_denied() {
            Long senderId = 99L;
            DmSendRequest request = new DmSendRequest(10L, "안녕!");

            given(conversationService.getConversationOrThrow(10L, senderId))
                    .willThrow(new ConversationException(
                            ConversationErrorCode.CONVERSATION_ACCESS_DENIED));

            assertThatThrownBy(() -> directMessageService.sendMessage(senderId, request))
                    .isInstanceOf(ConversationException.class);

            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("markMessagesAsRead()")
    class MarkMessagesAsRead {

        @Test
        @DisplayName("실패 - 참여자가 아니면 예외, 리포지토리 호출 안 함")
        void fail_when_not_participant() {
            given(conversationService.getConversationOrThrow(10L, 99L))
                    .willThrow(new ConversationException(
                            ConversationErrorCode.CONVERSATION_ACCESS_DENIED));

            assertThatThrownBy(() -> directMessageService.markMessagesAsRead(10L, 99L))
                    .isInstanceOf(ConversationException.class);

            then(directMessageRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 권한 검증 후 벌크 UPDATE 쿼리의 변경 행수를 그대로 반환한다")
        void success_returns_updated_count() {
            Member me = buildMember(1L, "koo");
            Member other = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, me, other);

            given(conversationService.getConversationOrThrow(10L, 1L))
                    .willReturn(conversation);
            given(directMessageRepository.markAllAsRead(10L, 1L)).willReturn(7);

            int updated = directMessageService.markMessagesAsRead(10L, 1L);

            assertThat(updated).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("getMessages()")
    class GetMessages {

        @Test
        @DisplayName("실패 - 참여자가 아니면 예외, 리포지토리 호출 안 함")
        void fail_when_not_participant() {
            given(conversationService.getConversationOrThrow(10L, 99L))
                    .willThrow(new ConversationException(
                            ConversationErrorCode.CONVERSATION_ACCESS_DENIED));

            assertThatThrownBy(() -> directMessageService.getMessages(10L, 99L, null, 20))
                    .isInstanceOf(ConversationException.class);

            then(directMessageRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 권한 검증 후 리포지토리 결과를 그대로 반환한다")
        void success_delegates_to_repository_after_permission_check() {
            Member me = buildMember(1L, "koo");
            Member other = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, me, other);

            given(conversationService.getConversationOrThrow(10L, 1L))
                    .willReturn(conversation);

            SliceResponse<DirectMessageResponse> expected = SliceResponse.of(
                    false,
                    List.of(new DirectMessageResponse(
                            50L, 10L, 1L, "koo", "hi", LocalDateTime.now())));
            given(directMessageRepository.findMessages(10L, 33L, 20)).willReturn(expected);

            SliceResponse<DirectMessageResponse> actual =
                    directMessageService.getMessages(10L, 1L, 33L, 20);

            assertThat(actual).isSameAs(expected);
        }
    }
}
