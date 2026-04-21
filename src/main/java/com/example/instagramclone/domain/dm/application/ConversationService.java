package com.example.instagramclone.domain.dm.application;

import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.core.exception.ConversationErrorCode;
import com.example.instagramclone.core.exception.ConversationException;
import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.ConversationRepository;
import com.example.instagramclone.domain.dm.domain.DirectMessageRepository;
import com.example.instagramclone.domain.dm.dto.ConversationResponse;
import com.example.instagramclone.domain.member.application.MemberService;
import com.example.instagramclone.domain.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final MemberService memberService;

    /**
     * 대화방 생성 또는 기존 반환(멱등).
     * - 자기 자신과는 대화 불가
     * - 이미 존재하면 기존 대화방 반환
     */
    @Transactional
    public ConversationResponse getOrCreateConversation(Long loginMemberId, Long targetMemberId) {
        if (loginMemberId.equals(targetMemberId)) {
            throw new ConversationException(ConversationErrorCode.SELF_CONVERSATION);
        }

        Conversation conversation = conversationRepository
                .findByParticipants(loginMemberId, targetMemberId)
                .orElseGet(() -> {
                    Member me = memberService.getReferenceById(loginMemberId);
                    Member target = memberService.findById(targetMemberId);
                    return conversationRepository.save(Conversation.create(me, target));
                });

        return ConversationResponse.from(conversation, loginMemberId);
    }

    /**
     * 내 대화방 목록을 커서 페이지네이션으로 조회 (conversation.id DESC).
     * 대화방당 최근 메시지 1건을 추가 조회해 DM 목록 미리보기에 노출한다.
     *
     * NOTE: 페이지 내부(N=size)에서만 N+1 이 발생하므로 상한이 있다. 대화방 수가
     * 커져도 한 페이지 크기만큼으로 제한되며, 추후 lastMessage 비정규화나
     * 한 방 쿼리(서브쿼리/CTE) 로 전환해 제거 가능하다.
     */
    public SliceResponse<ConversationResponse> getMyConversations(
            Long loginMemberId, Long cursorId, int size) {

        List<Conversation> raw =
                conversationRepository.findSliceByMemberId(loginMemberId, cursorId, size);

        boolean hasNext = raw.size() > size;
        List<Conversation> page = hasNext ? raw.subList(0, size) : raw;

        List<ConversationResponse> items = page.stream()
                .map(c -> {
                    var lastDm = directMessageRepository
                            .findTop1ByConversationOrderByCreatedAtDesc(c)
                            .orElse(null);
                    return ConversationResponse.from(c, loginMemberId, lastDm);
                })
                .toList();

        return SliceResponse.of(hasNext, items);
    }

    /**
     * 대화방 삭제 (하드 삭제).
     * - 참여자만 삭제 가능 (getConversationOrThrow 에서 권한 검증)
     * - 자식 메시지를 먼저 제거해 FK 제약을 해소한 뒤 대화방을 삭제한다
     *
     * DM 은 민감한 개인 데이터이므로 GDPR Right to Erasure 관점에서도
     * 소프트 삭제보다 하드 삭제가 더 적합하다.
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long loginMemberId) {
        Conversation conversation = getConversationOrThrow(conversationId, loginMemberId);
        directMessageRepository.deleteAllByConversation(conversation);
        conversationRepository.delete(conversation);
    }

    public Conversation getConversationOrThrow(Long conversationId, Long loginMemberId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException(
                        ConversationErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.isParticipant(loginMemberId)) {
            throw new ConversationException(ConversationErrorCode.CONVERSATION_ACCESS_DENIED);
        }

        return conversation;
    }
}
