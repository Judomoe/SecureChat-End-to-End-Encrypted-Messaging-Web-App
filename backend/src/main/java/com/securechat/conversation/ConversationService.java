package com.securechat.conversation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.contact.ContactService;
import com.securechat.exception.ResourceNotFoundException;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ContactService contactService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository conversationMemberRepository,
                               ContactService contactService) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.contactService = contactService;
    }

    public ConversationEntity createConversation(Long requesterId, Long recipientId) {
        if (!contactService.areContacts(requesterId, recipientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users are not contacts");
        }

        List<Long> existingIds = conversationMemberRepository.findCommonConversationIds(requesterId, recipientId);
        if (!existingIds.isEmpty()) {
            Optional<ConversationEntity> existing = conversationRepository.findById(existingIds.get(0));
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ConversationEntity conversation = new ConversationEntity();
        conversation = conversationRepository.save(conversation);

        ConversationMemberEntity member1 = new ConversationMemberEntity();
        member1.setConversationId(conversation.getId());
        member1.setUserId(requesterId);
        conversationMemberRepository.save(member1);

        ConversationMemberEntity member2 = new ConversationMemberEntity();
        member2.setConversationId(conversation.getId());
        member2.setUserId(recipientId);
        conversationMemberRepository.save(member2);

        return conversation;
    }

    public List<ConversationEntity> listConversations(Long userId) {
        List<Long> conversationIds = conversationMemberRepository.findConversationIdsByUserId(userId);
        return conversationIds.stream()
            .map(conversationRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toList());
    }

    public ConversationEntity getConversation(Long conversationId, Long userId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }

        return conversation;
    }

    public boolean isMember(Long conversationId, Long userId) {
        return conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    public List<ConversationMemberEntity> getMembers(Long conversationId) {
        return conversationMemberRepository.findByConversationId(conversationId);
    }
}
