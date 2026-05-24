package com.securechat.keyexchange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyMaterialRepository extends JpaRepository<KeyMaterialEntity, Long> {

    List<KeyMaterialEntity> findByRecipientIdAndConfirmedAndIsActive(Long recipientId, Boolean confirmed, Boolean isActive);

    Optional<KeyMaterialEntity> findByConversationIdAndRecipientIdAndIsActive(Long conversationId, Long recipientId, Boolean isActive);

    List<KeyMaterialEntity> findByConversationIdAndIsActive(Long conversationId, Boolean isActive);
}
