package com.securechat.message;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Page<MessageEntity> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<MessageEntity> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long beforeId, Pageable pageable);

    @Query("SELECT m FROM MessageEntity m WHERE m.conversationId = :conversationId ORDER BY m.id DESC")
    List<MessageEntity> findLastByConversationId(Long conversationId, Pageable pageable);
}
