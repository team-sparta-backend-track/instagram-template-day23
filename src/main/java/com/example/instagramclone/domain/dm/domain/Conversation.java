package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.core.common.BaseEntity;
import com.example.instagramclone.domain.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1:1 DM 대화방 엔티티.
 * 두 참여자(A, B) 조합은 유일하며, A-B / B-A 중복을 막기 위해
 * 서비스 계층에서 OR 조건으로 조회한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_participants",
                        columnNames = {"participant_a_id", "participant_b_id"}
                )
        }
)
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_a_id", nullable = false)
    private Member participantA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_b_id", nullable = false)
    private Member participantB;

    public static Conversation create(Member participantA, Member participantB) {
        Conversation conversation = new Conversation();
        conversation.participantA = participantA;
        conversation.participantB = participantB;
        return conversation;
    }

    public boolean isParticipant(Long memberId) {
        return participantA.getId().equals(memberId)
                || participantB.getId().equals(memberId);
    }

    public Member getOtherParticipant(Long myMemberId) {
        return participantA.getId().equals(myMemberId)
                ? participantB
                : participantA;
    }
}
