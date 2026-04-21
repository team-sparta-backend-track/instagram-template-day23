package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.domain.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversation 엔티티 단위 테스트.
 *
 * [테스트 범위]
 * - create(): participantA/B 세팅
 * - isParticipant(): 참여자 여부 판단
 * - getOtherParticipant(): 상대방 반환
 */
class ConversationTest {

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

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("두 참여자를 가진 Conversation 을 생성한다")
        void create_conversation_with_two_participants() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");

            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.getParticipantA()).isSameAs(a);
            assertThat(conversation.getParticipantB()).isSameAs(b);
        }
    }

    @Nested
    @DisplayName("isParticipant()")
    class IsParticipant {
        @Test
        @DisplayName("참여자 A 의 id 이면 true")
        void returns_true_for_participant_a() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.isParticipant(1L)).isTrue();
        }

        @Test
        @DisplayName("참여자 B 의 id 이면 true")
        void returns_true_for_participant_b() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.isParticipant(2L)).isTrue();
        }

        @Test
        @DisplayName("참여자가 아닌 id 이면 false")
        void returns_false_for_non_participant() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.isParticipant(99L)).isFalse();
        }
    }

    @Nested
    @DisplayName("getOtherParticipant()")
    class GetOtherParticipant {
        @Test
        @DisplayName("내가 A 이면 B 를 반환한다")
        void returns_b_when_i_am_a() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.getOtherParticipant(1L)).isSameAs(b);
        }

        @Test
        @DisplayName("내가 B 이면 A 를 반환한다")
        void returns_a_when_i_am_b() {
            Member a = buildMember(1L, "koo");
            Member b = buildMember(2L, "kuromi");
            Conversation conversation = Conversation.create(a, b);

            assertThat(conversation.getOtherParticipant(2L)).isSameAs(a);
        }
    }
}
