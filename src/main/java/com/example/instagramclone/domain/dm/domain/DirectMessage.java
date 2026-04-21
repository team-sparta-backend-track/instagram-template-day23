package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.core.common.BaseEntity;
import com.example.instagramclone.domain.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DM 대화방 안의 단일 메시지 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_messages")
public class DirectMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(nullable = false, length = 1000)
    private String content;

    /**
     * 수신자가 대화방에 진입해 메시지를 확인하면 true 로 전환된다.
     * primitive 로 선언하여 DB 제약과 "모르겠음" 상태를 원천 차단한다.
     */
    @Column(nullable = false)
    private boolean isRead = false;

    public static DirectMessage create(Conversation conversation, Member sender, String content) {
        DirectMessage message = new DirectMessage();
        message.conversation = conversation;
        message.sender = sender;
        message.content = content;
        return message;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
