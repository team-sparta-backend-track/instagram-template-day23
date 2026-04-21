package com.example.instagramclone.domain.dm.infrastructure;

import com.example.instagramclone.domain.dm.domain.Conversation;

import java.util.List;

/**
 * ConversationRepository 의 QueryDSL 동적 조회 전용 인터페이스.
 * Spring Data JPA 가 실행시점에 *Impl 구현을 자동으로 연결한다.
 */
public interface ConversationRepositoryCustom {

    /**
     * 내가 참여한 대화방을 커서 페이지네이션으로 조회한다.
     * 정렬 기준은 대화방 id DESC (최신 생성 우선) — BIGINT IDENTITY 라 단조 증가하여 안정적인 커서가 된다.
     *
     * @param memberId 로그인 사용자 id
     * @param cursorId 이전 페이지 마지막 대화방 id — null 이면 첫 페이지
     * @param size     페이지 크기. hasNext 판정을 위해 호출 측에서 +1 한 리스트를 받아 처리한다.
     * @return size+1 까지 포함될 수 있는 원시 목록. hasNext/잘라내기는 상위 계층에서 수행.
     */
    List<Conversation> findSliceByMemberId(Long memberId, Long cursorId, int size);
}
