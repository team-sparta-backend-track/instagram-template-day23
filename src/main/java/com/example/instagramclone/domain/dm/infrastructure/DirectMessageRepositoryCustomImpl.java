package com.example.instagramclone.domain.dm.infrastructure;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.dm.domain.QDirectMessage;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.domain.member.domain.QMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QDirectMessage dm = QDirectMessage.directMessage;
    private static final QMember sender = QMember.member;

    @Override
    public SliceResponse<DirectMessageResponse> findMessages(
            Long conversationId, Long cursorId, int size) {

        List<DirectMessage> results = queryFactory
                .selectFrom(dm)
                .join(dm.sender, sender).fetchJoin()   // senderUsername 접근 시 N+1 방지
                .where(
                        dm.conversation.id.eq(conversationId),
                        ltCursorId(cursorId)
                )
                .orderBy(dm.id.desc())
                .limit(size + 1L)                       // hasNext 판정용 +1
                .fetch();

        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);
        }

        List<DirectMessageResponse> content = results.stream()
                .map(DirectMessageResponse::from)
                .toList();

        return SliceResponse.of(hasNext, content);
    }

    /** cursorId 가 null 이면 첫 페이지 — 조건 미적용 */
    private BooleanExpression ltCursorId(Long cursorId) {
        return cursorId != null ? dm.id.lt(cursorId) : null;
    }
}
