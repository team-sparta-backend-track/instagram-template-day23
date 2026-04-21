package com.example.instagramclone.domain.dm.domain;

import com.example.instagramclone.domain.dm.infrastructure.ConversationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long>, ConversationRepositoryCustom {

    /**
     * 두 참여자 조합(A-B 또는 B-A)에 해당하는 대화방을 조회한다.
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.participantA.id = :memberA AND c.participantB.id = :memberB)
               OR (c.participantA.id = :memberB AND c.participantB.id = :memberA)
            """)
    Optional<Conversation> findByParticipants(
            @Param("memberA") Long memberAId,
            @Param("memberB") Long memberBId
    );
}
