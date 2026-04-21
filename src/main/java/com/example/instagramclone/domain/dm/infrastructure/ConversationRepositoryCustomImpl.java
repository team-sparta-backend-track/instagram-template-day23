package com.example.instagramclone.domain.dm.infrastructure;

import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.QConversation;
import com.example.instagramclone.domain.member.domain.QMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QConversation conversation = QConversation.conversation;
    private static final QMember participantA = new QMember("participantA");
    private static final QMember participantB = new QMember("participantB");

    @Override
    public List<Conversation> findSliceByMemberId(Long memberId, Long cursorId, int size) {
        return queryFactory
                .selectFrom(conversation)
                .join(conversation.participantA, participantA).fetchJoin()
                .join(conversation.participantB, participantB).fetchJoin()
                .where(
                        conversation.participantA.id.eq(memberId)
                                .or(conversation.participantB.id.eq(memberId)),
                        ltCursorId(cursorId)
                )
                .orderBy(conversation.id.desc())
                .limit(size + 1L)                   // hasNext 판정용 +1
                .fetch();
    }

    /** cursorId 가 null 이면 첫 페이지 — 조건 미적용 */
    private BooleanExpression ltCursorId(Long cursorId) {
        return cursorId != null ? conversation.id.lt(cursorId) : null;
    }
}
