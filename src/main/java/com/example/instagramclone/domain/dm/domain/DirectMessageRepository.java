package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.domain.dm.infrastructure.DirectMessageRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DirectMessageRepository
        extends JpaRepository<DirectMessage, Long>, DirectMessageRepositoryCustom {

    /**
     * 대화방 삭제 전 자식 메시지들을 모두 제거한다 (FK 제약).
     * 메시지 양이 많은 대화방에서는 @Modifying + JPQL 벌크 delete 로 교체 고려.
     */
    void deleteAllByConversation(Conversation conversation);

    /**
     * 대화방의 가장 최근 메시지 1건. DM 목록의 "미리보기" 용도.
     * Spring Data 메서드 이름 규칙: Top1 = LIMIT 1, OrderByCreatedAtDesc = 최신순.
     */
    Optional<DirectMessage> findTop1ByConversationOrderByCreatedAtDesc(Conversation conversation);

    /**
     * 대화방 내 "상대방이 보낸 읽지 않은 메시지" 를 한 번의 UPDATE 로 모두 읽음 처리.
     *
     * - {@code @Modifying(clearAutomatically=true)}: 벌크 연산이 1차 캐시를 우회하므로,
     *   이후 동일 엔티티 조회 시 DB/캐시 불일치가 생기지 않도록 영속성 컨텍스트를 비운다.
     * - sender.id != :memberId: 자기가 보낸 메시지는 이미 "읽은 것" 이므로 대상에서 제외.
     *
     * @return 실제로 상태가 변경된 메시지 수
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE DirectMessage dm
            SET dm.isRead = true
            WHERE dm.conversation.id = :conversationId
              AND dm.sender.id <> :memberId
              AND dm.isRead = false
            """)
    int markAllAsRead(@Param("conversationId") Long conversationId,
                      @Param("memberId") Long memberId);
}
