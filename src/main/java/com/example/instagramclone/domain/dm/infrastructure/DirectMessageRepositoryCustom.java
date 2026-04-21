package com.example.instagramclone.domain.dm.infrastructure;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;

/**
 * DirectMessageRepository 의 QueryDSL 동적 조회 전용 인터페이스.
 * Spring Data JPA 가 실행시점에 *Impl 구현을 자동으로 연결한다.
 */
public interface DirectMessageRepositoryCustom {

    /**
     * 대화방의 메시지를 커서 페이지네이션으로 조회한다 (최신 → 과거).
     *
     * @param conversationId 대화방 PK (필수)
     * @param cursorId       이전 페이지 마지막 메시지 id — null 이면 첫 페이지
     * @param size           페이지 크기
     */
    SliceResponse<DirectMessageResponse> findMessages(Long conversationId, Long cursorId, int size);
}
