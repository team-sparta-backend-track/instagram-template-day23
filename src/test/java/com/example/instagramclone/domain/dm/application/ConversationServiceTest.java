package com.example.instagramclone.domain.dm.application;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.core.exception.ConversationErrorCode;
import com.example.instagramclone.core.exception.ConversationException;
import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.ConversationRepository;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.dm.domain.DirectMessageRepository;
import com.example.instagramclone.domain.dm.dto.ConversationResponse;
import com.example.instagramclone.domain.member.application.MemberService;
import com.example.instagramclone.domain.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * ConversationService 단위 테스트.
 *
 * [테스트 범위]
 * - getOrCreateConversation(): 자기 자신 대화 방지, 멱등성(기존 반환), 신규 저장
 * - getMyConversations(): 내 대화방을 ConversationResponse 목록으로 변환
 * - getConversationOrThrow(): 없음/권한 없음 예외, 참여자면 반환
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private ConversationService conversationService;

    private Member buildMember(Long id, String username) {
        Member member = Member.builder()
                .username(username)
                .password("encoded_pw")
                .email(username + "@test.com")
                .name("테스트 유저")
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
    @DisplayName("getOrCreateConversation()")
    class GetOrCreate {

        @Test
        @DisplayName("실패 - 자기 자신과의 대화 요청이면 SELF_CONVERSATION 예외")
        void fail_self_conversation() {
            Long me = 1L;

            assertThatThrownBy(() -> conversationService.getOrCreateConversation(me, me))
                    .isInstanceOf(ConversationException.class)
                    .hasMessage(ConversationErrorCode.SELF_CONVERSATION.getMessage());

            then(conversationRepository).shouldHaveNoInteractions();
            then(memberService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("멱등 - 이미 존재하는 대화방이면 저장 없이 기존 대화방으로 응답한다")
        void idempotent_returns_existing() {
            Long meId = 1L;
            Long targetId = 2L;
            Member meMember = buildMember(meId, "koo");
            Member targetMember = buildMember(targetId, "kuromi");
            Conversation existing = buildConversation(10L, meMember, targetMember);

            given(conversationRepository.findByParticipants(meId, targetId))
                    .willReturn(Optional.of(existing));

            ConversationResponse response =
                    conversationService.getOrCreateConversation(meId, targetId);

            assertThat(response.conversationId()).isEqualTo(10L);
            assertThat(response.otherMemberId()).isEqualTo(targetId);
            assertThat(response.otherMemberUsername()).isEqualTo("kuromi");

            then(conversationRepository).should(never()).save(any(Conversation.class));
        }

        @Test
        @DisplayName("성공 - 기존 대화방이 없으면 새로 저장하고 응답한다")
        void success_creates_new_conversation() {
            Long meId = 1L;
            Long targetId = 2L;
            Member meMember = buildMember(meId, "koo");
            Member targetMember = buildMember(targetId, "kuromi");

            given(conversationRepository.findByParticipants(meId, targetId))
                    .willReturn(Optional.empty());
            given(memberService.getReferenceById(meId)).willReturn(meMember);
            given(memberService.findById(targetId)).willReturn(targetMember);

            Conversation saved = buildConversation(77L, meMember, targetMember);
            given(conversationRepository.save(any(Conversation.class))).willReturn(saved);

            ConversationResponse response =
                    conversationService.getOrCreateConversation(meId, targetId);

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            then(conversationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getParticipantA()).isSameAs(meMember);
            assertThat(captor.getValue().getParticipantB()).isSameAs(targetMember);

            assertThat(response.conversationId()).isEqualTo(77L);
            assertThat(response.otherMemberId()).isEqualTo(targetId);
        }
    }

    @Nested
    @DisplayName("getMyConversations()")
    class GetMyConversations {

        @Test
        @DisplayName("성공 - 각 대화방의 마지막 메시지를 함께 담아 SliceResponse 로 반환한다 (hasNext=false)")
        void returns_conversations_with_last_message_preview() {
            Long meId = 1L;
            Member meMember = buildMember(meId, "koo");
            Member kuromi = buildMember(2L, "kuromi");
            Member mamel = buildMember(3L, "mamel");

            Conversation c1 = buildConversation(10L, meMember, kuromi);
            Conversation c2 = buildConversation(11L, mamel, meMember);

            DirectMessage lastOfC1 = DirectMessage.create(c1, kuromi, "점심 뭐 먹?");
            // c2 에는 메시지가 아직 없다고 가정

            given(conversationRepository.findSliceByMemberId(meId, null, 20))
                    .willReturn(List.of(c1, c2));
            given(directMessageRepository.findTop1ByConversationOrderByCreatedAtDesc(c1))
                    .willReturn(Optional.of(lastOfC1));
            given(directMessageRepository.findTop1ByConversationOrderByCreatedAtDesc(c2))
                    .willReturn(Optional.empty());

            SliceResponse<ConversationResponse> slice =
                    conversationService.getMyConversations(meId, null, 20);

            assertThat(slice.hasNext()).isFalse();
            assertThat(slice.items()).hasSize(2);
            assertThat(slice.items().get(0).otherMemberUsername()).isEqualTo("kuromi");
            assertThat(slice.items().get(0).lastMessage()).isEqualTo("점심 뭐 먹?");
            assertThat(slice.items().get(1).otherMemberUsername()).isEqualTo("mamel");
            assertThat(slice.items().get(1).lastMessage()).isNull();
            assertThat(slice.items().get(1).lastMessageAt()).isNull();
        }

        @Test
        @DisplayName("성공 - 레포가 size+1 건을 반환하면 마지막 하나를 잘라내고 hasNext=true")
        void detects_has_next_when_repo_returns_size_plus_one() {
            Long meId = 1L;
            Member meMember = buildMember(meId, "koo");
            Member kuromi = buildMember(2L, "kuromi");
            Member mamel = buildMember(3L, "mamel");

            Conversation c1 = buildConversation(10L, meMember, kuromi);
            Conversation c2 = buildConversation(11L, mamel, meMember);

            // size=1 에 대해 레포는 hasNext 판정용 +1 즉 2건 반환
            given(conversationRepository.findSliceByMemberId(meId, null, 1))
                    .willReturn(List.of(c1, c2));
            given(directMessageRepository.findTop1ByConversationOrderByCreatedAtDesc(c1))
                    .willReturn(Optional.empty());

            SliceResponse<ConversationResponse> slice =
                    conversationService.getMyConversations(meId, null, 1);

            assertThat(slice.hasNext()).isTrue();
            assertThat(slice.items()).hasSize(1);
            assertThat(slice.items().get(0).conversationId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getConversationOrThrow()")
    class GetConversationOrThrow {

        @Test
        @DisplayName("실패 - 대화방이 없으면 CONVERSATION_NOT_FOUND 예외")
        void fail_not_found() {
            given(conversationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.getConversationOrThrow(99L, 1L))
                    .isInstanceOf(ConversationException.class)
                    .hasMessage(ConversationErrorCode.CONVERSATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 참여자가 아니면 CONVERSATION_ACCESS_DENIED 예외")
        void fail_access_denied() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, a, b);

            given(conversationRepository.findById(10L)).willReturn(Optional.of(conversation));

            assertThatThrownBy(() -> conversationService.getConversationOrThrow(10L, 99L))
                    .isInstanceOf(ConversationException.class)
                    .hasMessage(ConversationErrorCode.CONVERSATION_ACCESS_DENIED.getMessage());
        }

        @Test
        @DisplayName("성공 - 참여자이면 대화방을 반환한다")
        void success_returns_conversation_for_participant() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, a, b);

            given(conversationRepository.findById(10L)).willReturn(Optional.of(conversation));

            Conversation result = conversationService.getConversationOrThrow(10L, 1L);

            assertThat(result).isSameAs(conversation);
        }
    }

    @Nested
    @DisplayName("deleteConversation()")
    class DeleteConversation {

        @Test
        @DisplayName("실패 - 대화방이 없으면 CONVERSATION_NOT_FOUND 예외")
        void fail_not_found() {
            given(conversationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.deleteConversation(99L, 1L))
                    .isInstanceOf(ConversationException.class)
                    .hasMessage(ConversationErrorCode.CONVERSATION_NOT_FOUND.getMessage());

            then(directMessageRepository).shouldHaveNoInteractions();
            then(conversationRepository).should(never()).delete(any(Conversation.class));
        }

        @Test
        @DisplayName("실패 - 참여자가 아니면 CONVERSATION_ACCESS_DENIED 예외 + 아무것도 삭제하지 않는다")
        void fail_access_denied_does_not_delete() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, a, b);

            given(conversationRepository.findById(10L)).willReturn(Optional.of(conversation));

            assertThatThrownBy(() -> conversationService.deleteConversation(10L, 99L))
                    .isInstanceOf(ConversationException.class)
                    .hasMessage(ConversationErrorCode.CONVERSATION_ACCESS_DENIED.getMessage());

            then(directMessageRepository).shouldHaveNoInteractions();
            then(conversationRepository).should(never()).delete(any(Conversation.class));
        }

        @Test
        @DisplayName("성공 - 메시지 먼저 삭제 후 대화방 삭제 (FK 제약)")
        void success_deletes_messages_before_conversation() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = buildConversation(10L, a, b);

            given(conversationRepository.findById(10L)).willReturn(Optional.of(conversation));

            conversationService.deleteConversation(10L, 1L);

            InOrder order = inOrder(directMessageRepository, conversationRepository);
            order.verify(directMessageRepository).deleteAllByConversation(conversation);
            order.verify(conversationRepository).delete(conversation);
        }
    }
}
