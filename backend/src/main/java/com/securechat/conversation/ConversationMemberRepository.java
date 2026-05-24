package com.securechat.conversation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, Long> {

    List<ConversationMemberEntity> findByConversationId(Long conversationId);

    List<ConversationMemberEntity> findByUserId(Long userId);

    @Query("SELECT DISTINCT cm.conversationId FROM ConversationMemberEntity cm WHERE cm.userId = :userId")
    List<Long> findConversationIdsByUserId(Long userId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT a.conversationId FROM ConversationMemberEntity a WHERE a.userId = :user1 AND a.conversationId IN (SELECT b.conversationId FROM ConversationMemberEntity b WHERE b.userId = :user2)")
    List<Long> findCommonConversationIds(Long user1, Long user2);
}
