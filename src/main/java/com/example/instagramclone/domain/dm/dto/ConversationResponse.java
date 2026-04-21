package com.example.instagramclone.domain.dm.dto;

import com.example.instagramclone.domain.dm.domain.Conversation;
import com.example.instagramclone.domain.dm.domain.DirectMessage;
import com.example.instagramclone.domain.member.domain.Member;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long conversationId,
        Long otherMemberId,
        String otherMemberUsername,
        String otherMemberProfileImageUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
    /**
     * 마지막 메시지가 없는 뷰(생성 직후 등)에서 사용.
     */
    public static ConversationResponse from(Conversation conversation, Long myMemberId) {
        return from(conversation, myMemberId, null);
    }

    /**
     * DM 목록용 — 마지막 메시지 미리보기를 포함한다. lastDm 은 nullable.
     */
    public static ConversationResponse from(Conversation conversation,
                                            Long myMemberId,
                                            DirectMessage lastDm) {
        Member other = conversation.getOtherParticipant(myMemberId);
        return new ConversationResponse(
                conversation.getId(),
                other.getId(),
                other.getUsername(),
                other.getProfileImageUrl(),
                lastDm != null ? lastDm.getContent() : null,
                lastDm != null ? lastDm.getCreatedAt() : null,
                conversation.getCreatedAt()
        );
    }
}
