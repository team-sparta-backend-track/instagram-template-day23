package com.example.instagramclone.domain.dm.infrastructure;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.ConversationRepository;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.dm.domain.DirectMessageRepository;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.member.domain.Member;
import com.example.instagramclone.domain.member.domain.MemberRepository;
import com.example.instagramclone.domain.post.infrastructure.PostGridQueryHelper;
import com.example.instagramclone.infrastructure.persistence.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DirectMessageRepositoryCustom 통합 테스트.
 *
 * [테스트 범위]
 * - conversationId 로 대화방별 메시지만 반환 (크로스 노출 방지)
 * - id desc 정렬 (최신 메시지 먼저)
 * - size+1 트릭으로 hasNext 판정 (COUNT 없이)
 * - cursorId null → 첫 페이지, cursorId 있음 → 이전 페이지
 */
@DataJpaTest
@Import({QueryDslConfig.class, PostGridQueryHelper.class})
class DirectMessageRepositoryCustomImplTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member koo;
    private Member kuromi;
    private Conversation conversation;
    private List<DirectMessage> saved;

    @BeforeEach
    void setUp() {
        koo = memberRepository.save(build("koo"));
        kuromi = memberRepository.save(build("kuromi"));
        conversation = conversationRepository.save(Conversation.create(koo, kuromi));

        // 5개 메시지 저장 (저장 순서 = id 순서)
        saved = List.of(
                directMessageRepository.save(DirectMessage.create(conversation, koo, "msg1")),
                directMessageRepository.save(DirectMessage.create(conversation, kuromi, "msg2")),
                directMessageRepository.save(DirectMessage.create(conversation, koo, "msg3")),
                directMessageRepository.save(DirectMessage.create(conversation, kuromi, "msg4")),
                directMessageRepository.save(DirectMessage.create(conversation, koo, "msg5"))
        );
    }

    private Member build(String username) {
        return Member.builder()
                .username(username).password("pw")
                .email(username + "@test.com").name(username)
                .build();
    }

    @Test
    @DisplayName("첫 페이지(cursor=null) - 최신 메시지부터 size 개 반환, hasNext=true")
    void first_page_returns_latest_with_has_next() {
        SliceResponse<DirectMessageResponse> slice =
                directMessageRepository.findMessages(conversation.getId(), null, 3);

        assertThat(slice.items()).extracting(DirectMessageResponse::content)
                .containsExactly("msg5", "msg4", "msg3");
        assertThat(slice.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 페이지 - cursor 이후(=id 더 작은) 메시지만 반환")
    void next_page_uses_cursor_to_fetch_older_messages() {
        Long cursor = saved.get(2).getId(); // msg3 의 id — 이보다 작은 id 만 조회

        SliceResponse<DirectMessageResponse> slice =
                directMessageRepository.findMessages(conversation.getId(), cursor, 3);

        assertThat(slice.items()).extracting(DirectMessageResponse::content)
                .containsExactly("msg2", "msg1");
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("정확히 size 만큼 있으면 hasNext=false")
    void exact_size_means_no_next() {
        SliceResponse<DirectMessageResponse> slice =
                directMessageRepository.findMessages(conversation.getId(), null, 5);

        assertThat(slice.items()).hasSize(5);
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("다른 대화방 메시지는 섞이지 않는다")
    void does_not_leak_other_conversation_messages() {
        Member mamel = memberRepository.save(build("mamel"));
        Conversation other = conversationRepository.save(Conversation.create(koo, mamel));
        directMessageRepository.save(DirectMessage.create(other, koo, "other-1"));
        directMessageRepository.save(DirectMessage.create(other, mamel, "other-2"));

        SliceResponse<DirectMessageResponse> slice =
                directMessageRepository.findMessages(conversation.getId(), null, 10);

        assertThat(slice.items()).extracting(DirectMessageResponse::content)
                .containsExactly("msg5", "msg4", "msg3", "msg2", "msg1");
    }
}
