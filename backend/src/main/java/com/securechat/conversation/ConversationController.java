package com.securechat.conversation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securechat.message.MessageEntity;
import com.securechat.message.MessageRepository;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ConversationController(ConversationService conversationService,
                                  ConversationMemberRepository conversationMemberRepository,
                                  UserRepository userRepository,
                                  MessageRepository messageRepository) {
        this.conversationService = conversationService;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(@RequestBody CreateConversationRequest request) {
        Long userId = getCurrentUserId();
        ConversationEntity conversation = conversationService.createConversation(userId, request.getRecipientId());

        ConversationResponse response = toResponse(conversation);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations() {
        Long userId = getCurrentUserId();
        List<ConversationEntity> conversations = conversationService.listConversations(userId);

        List<ConversationResponse> responses = conversations.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        ConversationEntity conversation = conversationService.getConversation(id, userId);

        ConversationResponse response = toResponse(conversation);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    private ConversationResponse toResponse(ConversationEntity conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setCreatedAt(conversation.getCreatedAt() != null ? conversation.getCreatedAt().toString() : null);

        List<ConversationMemberEntity> members = conversationService.getMembers(conversation.getId());
        List<ConversationResponse.MemberDto> memberDtos = members.stream()
            .map(member -> {
                ConversationResponse.MemberDto dto = new ConversationResponse.MemberDto();
                dto.setUserId(member.getUserId());
                dto.setJoinedAt(member.getJoinedAt() != null ? member.getJoinedAt().toString() : null);

                userRepository.findById(member.getUserId()).ifPresent(user -> {
                    dto.setUsername(user.getUsername());
                });

                return dto;
            })
            .collect(Collectors.toList());

        response.setMembers(memberDtos);

        List<MessageEntity> lastMessages = messageRepository.findLastByConversationId(
            conversation.getId(), PageRequest.of(0, 1));
        if (!lastMessages.isEmpty()) {
            MessageEntity last = lastMessages.get(0);
            ConversationResponse.LastMessageDto lastMessage = new ConversationResponse.LastMessageDto();
            lastMessage.setSenderId(last.getSenderId());
            lastMessage.setTimestamp(last.getTimestamp());
            response.setLastMessage(lastMessage);
        }

        return response;
    }
}
