package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.domain.member.domain.Member;
import com.example.instagramclone.domain.member.domain.MemberRepository;
import com.example.instagramclone.domain.post.infrastructure.PostGridQueryHelper;
import com.example.instagramclone.infrastructure.persistence.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConversationRepository 통합 테스트 (@DataJpaTest / H2).
 * - findByParticipants(): A-B, B-A 두 순서 모두에서 같은 대화방을 찾는다.
 * - findSliceByMemberId(): 내가 참여한 대화방을 커서 페이지네이션(id DESC)으로 가져온다.
 */
@DataJpaTest
@Import({QueryDslConfig.class, PostGridQueryHelper.class})
class ConversationRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member koo;
    private Member kuromi;
    private Member mamel;

    @BeforeEach
    void setUp() {
        koo = memberRepository.save(buildMember("koo"));
        kuromi = memberRepository.save(buildMember("kuromi"));
        mamel = memberRepository.save(buildMember("mamel"));
    }

    private Member buildMember(String username) {
        return Member.builder()
                .username(username)
                .password("encoded_pw")
                .email(username + "@test.com")
                .name(username)
                .build();
    }

    @Nested
    @DisplayName("findByParticipants()")
    class FindByParticipants {

        @Test
        @DisplayName("A-B 순서로 저장한 대화방을 B-A 순서로도 찾는다")
        void finds_conversation_regardless_of_order() {
            Conversation saved = conversationRepository.save(Conversation.create(koo, kuromi));

            Optional<Conversation> forward = conversationRepository
                    .findByParticipants(koo.getId(), kuromi.getId());
            Optional<Conversation> reverse = conversationRepository
                    .findByParticipants(kuromi.getId(), koo.getId());

            assertThat(forward).isPresent()
                    .get().extracting(Conversation::getId).isEqualTo(saved.getId());
            assertThat(reverse).isPresent()
                    .get().extracting(Conversation::getId).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("참여자 조합이 일치하지 않으면 빈 Optional 을 반환한다")
        void returns_empty_when_not_found() {
            conversationRepository.save(Conversation.create(koo, kuromi));

            Optional<Conversation> result = conversationRepository
                    .findByParticipants(koo.getId(), mamel.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DirectMessageRepository.findTop1ByConversationOrderByCreatedAtDesc()")
    class FindTopLastMessage {

        @Test
        @DisplayName("대화방의 가장 최근 메시지 1건을 반환한다")
        void returns_most_recent_message() {
            Conversation c = conversationRepository.save(Conversation.create(koo, kuromi));
            directMessageRepository.save(DirectMessage.create(c, koo, "첫 메시지"));
            directMessageRepository.save(DirectMessage.create(c, kuromi, "두 번째"));
            DirectMessage last = directMessageRepository.save(
                    DirectMessage.create(c, koo, "마지막"));

            Optional<DirectMessage> result =
                    directMessageRepository.findTop1ByConversationOrderByCreatedAtDesc(c);

            assertThat(result).isPresent()
                    .get().extracting(DirectMessage::getId).isEqualTo(last.getId());
        }

        @Test
        @DisplayName("메시지가 없는 대화방이면 빈 Optional 을 반환한다")
        void returns_empty_when_no_messages() {
            Conversation c = conversationRepository.save(Conversation.create(koo, kuromi));

            Optional<DirectMessage> result =
                    directMessageRepository.findTop1ByConversationOrderByCreatedAtDesc(c);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findSliceByMemberId()")
    class FindSliceByMemberId {

        @Test
        @DisplayName("내가 참여한 대화방만 id DESC 순으로 반환한다 (커서 미지정)")
        void returns_only_my_conversations_desc() {
            Conversation c1 = conversationRepository.save(Conversation.create(koo, kuromi));   // 내 것
            Conversation c2 = conversationRepository.save(Conversation.create(koo, mamel));    // 내 것
            conversationRepository.save(Conversation.create(kuromi, mamel));                  // 남의 것

            List<Conversation> mine = conversationRepository
                    .findSliceByMemberId(koo.getId(), null, 10);

            assertThat(mine).hasSize(2);
            assertThat(mine).allSatisfy(c ->
                    assertThat(c.isParticipant(koo.getId())).isTrue());
            // id DESC — 나중에 생성된 c2 가 먼저
            assertThat(mine.get(0).getId()).isEqualTo(c2.getId());
            assertThat(mine.get(1).getId()).isEqualTo(c1.getId());
        }

        @Test
        @DisplayName("cursorId 이후(더 작은 id) 만 반환한다 — 커서 페이지네이션")
        void cursor_returns_only_older_conversations() {
            Conversation c1 = conversationRepository.save(Conversation.create(koo, kuromi));
            Conversation c2 = conversationRepository.save(Conversation.create(koo, mamel));

            // c2.id 를 커서로 — c2 는 제외되고 c1 만 반환
            List<Conversation> older = conversationRepository
                    .findSliceByMemberId(koo.getId(), c2.getId(), 10);

            assertThat(older).hasSize(1);
            assertThat(older.get(0).getId()).isEqualTo(c1.getId());
        }

        @Test
        @DisplayName("hasNext 판정용 +1 — size 초과분까지 함께 반환")
        void returns_size_plus_one_for_has_next() {
            conversationRepository.save(Conversation.create(koo, kuromi));
            conversationRepository.save(Conversation.create(koo, mamel));

            // size=1 요청이면 limit(size+1)=2 적용되어 2건 반환
            List<Conversation> page = conversationRepository
                    .findSliceByMemberId(koo.getId(), null, 1);

            assertThat(page).hasSize(2);
        }
    }

    @Nested
    @DisplayName("DirectMessageRepository.markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("상대방이 보낸 읽지 않은 메시지만 읽음 처리하고 갯수를 반환한다")
        void marks_only_others_unread_messages() {
            Conversation c = conversationRepository.save(Conversation.create(koo, kuromi));
            // 상대방(kuromi) 이 보낸 읽지 않은 메시지 2건 — 대상
            DirectMessage fromOther1 =
                    directMessageRepository.save(DirectMessage.create(c, kuromi, "hi"));
            DirectMessage fromOther2 =
                    directMessageRepository.save(DirectMessage.create(c, kuromi, "안녕"));
            // 내가(koo) 보낸 메시지 1건 — 제외 대상
            DirectMessage fromMe =
                    directMessageRepository.save(DirectMessage.create(c, koo, "응!"));

            int updated = directMessageRepository.markAllAsRead(c.getId(), koo.getId());

            assertThat(updated).isEqualTo(2);
            // clearAutomatically=true 에 의해 1차 캐시가 비워져 DB 에서 재조회됨
            assertThat(directMessageRepository.findById(fromOther1.getId()))
                    .get().extracting(DirectMessage::isRead).isEqualTo(true);
            assertThat(directMessageRepository.findById(fromOther2.getId()))
                    .get().extracting(DirectMessage::isRead).isEqualTo(true);
            assertThat(directMessageRepository.findById(fromMe.getId()))
                    .get().extracting(DirectMessage::isRead).isEqualTo(false);
        }

        @Test
        @DisplayName("이미 읽음인 메시지는 다시 UPDATE 대상에서 제외된다 (멱등)")
        void idempotent_skips_already_read() {
            Conversation c = conversationRepository.save(Conversation.create(koo, kuromi));
            DirectMessage m1 = directMessageRepository.save(DirectMessage.create(c, kuromi, "hi"));
            m1.markAsRead();
            directMessageRepository.save(m1);
            directMessageRepository.save(DirectMessage.create(c, kuromi, "안녕")); // unread

            int updated = directMessageRepository.markAllAsRead(c.getId(), koo.getId());

            assertThat(updated).isEqualTo(1); // 두 번째 것만 갱신
        }

        @Test
        @DisplayName("다른 대화방 메시지는 영향을 받지 않는다")
        void does_not_touch_other_conversations() {
            Conversation mine = conversationRepository.save(Conversation.create(koo, kuromi));
            Conversation other = conversationRepository.save(Conversation.create(kuromi, mamel));

            directMessageRepository.save(DirectMessage.create(mine, kuromi, "mine-1"));
            DirectMessage otherMsg =
                    directMessageRepository.save(DirectMessage.create(other, kuromi, "other-1"));

            directMessageRepository.markAllAsRead(mine.getId(), koo.getId());

            assertThat(directMessageRepository.findById(otherMsg.getId()))
                    .get().extracting(DirectMessage::isRead).isEqualTo(false);
        }
    }
}
