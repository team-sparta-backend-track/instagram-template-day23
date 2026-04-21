package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.domain.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DirectMessage 엔티티 단위 테스트.
 * create() 로 conversation / sender / content 를 세팅한다.
 */
class DirectMessageTest {

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

    @Test
    @DisplayName("create() - conversation, sender, content 를 세팅한다")
    void create_direct_message() {
        Member a = buildMember(1L, "koo");
        Member b = buildMember(2L, "kuromi");
        Conversation conversation = Conversation.create(a, b);

        DirectMessage message = DirectMessage.create(conversation, a, "안녕!");

        assertThat(message.getConversation()).isSameAs(conversation);
        assertThat(message.getSender()).isSameAs(a);
        assertThat(message.getContent()).isEqualTo("안녕!");
    }

    @Test
    @DisplayName("create() - 새 메시지는 기본적으로 읽지 않은 상태(isRead=false)로 생성된다")
    void create_message_is_unread_by_default() {
        Member a = buildMember(1L, "koo");
        Member b = buildMember(2L, "kuromi");
        Conversation conversation = Conversation.create(a, b);

        DirectMessage message = DirectMessage.create(conversation, a, "안녕!");

        assertThat(message.isRead()).isFalse();
    }

    @Test
    @DisplayName("markAsRead() - 호출하면 isRead 가 true 로 바뀐다")
    void mark_as_read_flips_flag() {
        Member a = buildMember(1L, "koo");
        Member b = buildMember(2L, "kuromi");
        Conversation conversation = Conversation.create(a, b);
        DirectMessage message = DirectMessage.create(conversation, a, "안녕!");

        message.markAsRead();

        assertThat(message.isRead()).isTrue();
    }
}
