package com.securechat.contact;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    List<ContactEntity> findByRequesterIdOrRecipientIdAndStatus(Long requesterId, Long recipientId, String status);

    List<ContactEntity> findByRequesterIdAndStatus(Long requesterId, String status);

    List<ContactEntity> findByRecipientIdAndStatus(Long recipientId, String status);

    boolean existsByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    @Query("SELECT c FROM ContactEntity c WHERE (c.requesterId = :userId OR c.recipientId = :userId) AND c.status = 'ACCEPTED'")
    List<ContactEntity> findAcceptedContacts(Long userId);

    Optional<ContactEntity> findById(Long id);
}
